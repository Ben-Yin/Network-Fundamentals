package dns;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import utils.CDNLogger;
import utils.NetworkUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by HappyMole on 11/29/16.
 */
public class MeasureController {

    private static final String REPLICA_HOSTS_PATH = "ec2_hosts";

    private int httpServerPort;
    private AsyncHttpClient asyncHttpClient;

    private Map<String, float[]> replicaServerInfo;
    //    private Map<String, Float> savedReplicaOriginLatencies;
    private Map<String, String[]> savedBestReplicaServers;

    public MeasureController(int httpServerPort) throws Exception {
        this.httpServerPort = httpServerPort;
        this.asyncHttpClient = new AsyncHttpClient();
        this.replicaServerInfo = loadReplicaServers();
        // key: client IP, value: best replica server IP and latency
        // need to be thread-safe
        this.savedBestReplicaServers = new Hashtable<String, String[]>();
    }

    public static void main(String[] args) throws Exception {
        // Boston
        System.out.println(new MeasureController(123).getNearestServerIp("129.10.9.53"));
        // Beihaidao
        System.out.println(new MeasureController(123).getNearestServerIp("133.50.0.17"));
        // Beijing
        System.out.println(new MeasureController(123).getNearestServerIp("58.30.15.255"));
    }

    private Map<String, float[]> loadReplicaServers() throws Exception {
        Map<String, float[]> replicasInfoMap = new HashMap<String, float[]>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(REPLICA_HOSTS_PATH));
            String line;
            while ((line = reader.readLine()) != null) {
                String host = line.substring(0, line.indexOf(" ")).trim();
                String ip = NetworkUtils.getIpByHost(host);
                float[] ipGeo = NetworkUtils.getGeoByIp(ip);
                replicasInfoMap.put(ip, ipGeo);
            }
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        return replicasInfoMap;
    }

    public String selectBestReplicaServer(final String clientIp) {
        if (savedBestReplicaServers.containsKey(clientIp)) {
            return savedBestReplicaServers.get(clientIp)[0];
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    probeReplicaLatencies(clientIp);
                }
            }).start();
            return getNearestServerIp(clientIp);
        }
    }

    private void probeReplicaLatencies(final String clientIp) {
        for (final String replicaServerIp : replicaServerInfo.keySet()) {
            String probeUrl = "http://" + replicaServerIp + ":" + this.httpServerPort + "/probeLatency?ip=" + clientIp;
            asyncHttpClient.prepareGet(probeUrl).execute(new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response response) throws Exception {
                    synchronized (savedBestReplicaServers) {
                        String latency = response.getResponseBody();
                        String[] replicaServerInfo = {replicaServerIp, latency};
                        if (!savedBestReplicaServers.containsKey(clientIp)) {
                            savedBestReplicaServers.put(clientIp, replicaServerInfo);
                        } else {
                            float currentLowestLatency = Float.parseFloat(savedBestReplicaServers.get(clientIp)[1]);
                            if (Float.parseFloat(latency) < currentLowestLatency) {
                                savedBestReplicaServers.put(clientIp, replicaServerInfo);
                            }
                        }
                    }
                    return response;
                }
            });
        }
    }

    private String getNearestServerIp(String clientIp) {
        float[] clientGeo = NetworkUtils.getGeoByIp(clientIp);

        String nearestServerIp = null;
        double minDistance = Double.MAX_VALUE;

        for (String replicaServerIp : replicaServerInfo.keySet()) {
            float[] replicaServerGeo = replicaServerInfo.get(replicaServerIp);
            double distance = Math.sqrt(Math.pow((replicaServerGeo[0] - clientGeo[0]), 2) + Math.pow((replicaServerGeo[1] - clientGeo[1]), 2));
            CDNLogger.info("the distance between <" + clientIp + "> and <" + replicaServerIp + "> is " + "\"" + distance + "\"");
            if (distance < minDistance) {
                minDistance = distance;
                nearestServerIp = replicaServerIp;
            }
        }
        return nearestServerIp;
    }
}
