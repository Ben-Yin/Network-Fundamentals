package utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by HappyMole on 11/25/16.
 */
public class NetworkUtils {

    private static final String ipGeoService = "ipinfo.io/";

    public static String getLocalIpAddress() {
        String ifconfigResult = executeCommand("/sbin/ifconfig");
        String ipLine = ifconfigResult.split("\n")[1].trim().split(" ")[1];
        if (ipLine.indexOf(":") == -1) {
            return ipLine;
        } else {
            return ipLine.substring(ipLine.indexOf(":") + 1);
        }
    }

    public static String getIpByHost(String host) {
        String ipAddress = null;

        try {
            ipAddress = InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException e) {
            CDNLogger.error("failed to get ip address for <" + host + ">");
        }

        return ipAddress;
    }

    public static float[] getGeoByIp(String ip) {
        String curlCommand = "curl " + ipGeoService + ip;
//        CDNLogger.info("get geo info of <" + ip + "> with command <" + curlCommand + ">");
        String curlResult = executeCommand(curlCommand);
        float[] geoCoordinates = new float[2];
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(curlResult);
            String[] location = ((String) jsonObject.get("loc")).split(",");
            geoCoordinates[0] = Float.parseFloat(location[0]);
            geoCoordinates[1] = Float.parseFloat(location[1]);
            return geoCoordinates;
        } catch (ParseException e) {
            CDNLogger.error("failed to get geo location for <" + ip + ">");
        }
        return geoCoordinates;
    }

    private static String executeCommand(String command) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(new String[]{"bash", "-c", command});

            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
            byte[] bytesResult = ArrayUtils.shiftToBytes(bufferedInputStream);
            return new String(bytesResult);
        } catch (IOException e) {
            CDNLogger.error("failed to execute the command: " + command);
        }

        return "";
    }

    public static float getLatency(String target) {
        String scamperCommand = "sudo scamper -c \"ping -c 5\" -i " + target;
        String[] scamperResult = executeCommand(scamperCommand).split("\n");
        String stat = scamperResult[scamperResult.length - 1];

        if (stat.startsWith("round-trip")) {
            float avgLatency = Float.parseFloat(stat.split("/")[4]);
            return avgLatency;
        } else {
            return Float.MAX_VALUE;
        }
    }

    public static void main(String[] args) {
        long before = System.currentTimeMillis();
        getGeoByIp("8.8.8.8");
        long after = System.currentTimeMillis();
        System.out.println(after - before);
        System.out.println(getLatency("168.235.251.30"));
    }
}
