package http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.CDNLogger;
import utils.HttpClient;
import utils.NetworkUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by HappyMole on 11/23/16.
 */
public class HttpServer {
    private static final int MAX_THREADS = 30;

    private String originServerHost;
    private int originServerPort;
    private int port;
    private Executor executor;

    public HttpServer(String originServer, int port) {
        parseHostAndPort(originServer);
        this.port = port;
        this.executor = Executors.newFixedThreadPool(MAX_THREADS);
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        // http://ec2-54-167-4-20.compute-1.amazonaws.com:8080
        String originServer = args[1];
        new HttpServer(originServer, port).start();
    }

    private void parseHostAndPort(String originServer) {
        if (originServer.startsWith("http://")) {
            originServer = originServer.substring("http://".length());
        }

        if (originServer.contains(":")) {
            int splitIndex = originServer.indexOf(":");
            this.originServerHost = originServer.substring(0, splitIndex);
            this.originServerPort = Integer.parseInt(originServer.substring(splitIndex + 1));
        } else {
            this.originServerHost = originServer;
            this.originServerPort = 80;
        }
    }

    public void start() {
        try {
            String localIpAddress = NetworkUtils.getLocalIpAddress();
            InetSocketAddress addr = new InetSocketAddress(localIpAddress, port);
            com.sun.net.httpserver.HttpServer httpServer = com.sun.net.httpserver.HttpServer.create(addr, 0);

            httpServer.setExecutor(executor);
            httpServer.createContext("/", new CacheableHttpHandler(originServerHost, originServerPort));
            httpServer.start();
            CDNLogger.info("#####HTTP server started!#####");
            CDNLogger.info("IP: " + localIpAddress + ", port: " + port);
        } catch (IOException e) {
            CDNLogger.error("failed to create a new http server, exit the program!");
            System.exit(-1);
        }
    }
}

class CacheableHttpHandler implements HttpHandler {
    private static final String PROBE_LATENCY_PATH = "/probeLatency";

    private String originServerHost;
    private int originServerPort;

    private LRUCache cache;
    private HttpClient httpProxyClient;
    private Executor executor;

    public CacheableHttpHandler(String originServerHost, int originServerPort) {
        this.originServerHost = originServerHost;
        this.originServerPort = originServerPort;

        this.cache = new LRUCache();
        this.httpProxyClient = new HttpClient();
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String requestMethod = httpExchange.getRequestMethod();
        if (requestMethod.equalsIgnoreCase("GET")) {
            String path = httpExchange.getRequestURI().getPath();
            if (path.equals("")) {
                path = "/index.html";
            }

            CDNLogger.info("received request for path: " + path);

            Headers responseHeaders = httpExchange.getResponseHeaders();
            responseHeaders.clear();

            byte[] responseData;
            if (path.equals(PROBE_LATENCY_PATH)) {
                responseHeaders.set("Content-Type", "text/plain");
                String query = httpExchange.getRequestURI().getQuery();
                String clientIp = query.split("=")[1];
                responseData = Float.toString(NetworkUtils.getLatency(clientIp)).getBytes();

            } else {
                responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
                // remove prefix "/"
                path = path.substring(1);
                responseData = getServerResponse(path);
            }

            BufferedOutputStream responseBody = new BufferedOutputStream(httpExchange.getResponseBody());
            if (responseData != null) {
                httpExchange.sendResponseHeaders(200, responseData.length);
                responseBody.write(responseData);
            } else {
                responseData = "invalid request".getBytes();
                httpExchange.sendResponseHeaders(404, responseData.length);
                responseBody.write(responseData);
            }
            responseBody.flush();
        }
    }
    /*
    private byte[] getServerResponse(final String path) {
        final ARCache.KEY_STATUS keyStatus = cache.exists(path);
        if (keyStatus == ARCache.KEY_STATUS.KEY_IN_LFU || keyStatus == ARCache.KEY_STATUS.KEY_IN_LRU) {
            // get response data from cache
            return cache.readCache(path, keyStatus);
        } else {
            // get response data from the origin server
            String url = "http://" + originServerHost + ":" + originServerPort + "/" + path;
            final byte[] responseData = httpProxyClient.doGet(url);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    cache.writeCache(path, responseData, keyStatus);
                }
            });
            return responseData;
        }
    }
    */

    private byte[] getServerResponse(final String path) {
        byte[] cacheData;
        if ((cacheData = cache.readCache(path)) != null) {
            // get response data from cache
            return cacheData;
        } else {
            // get response data from the origin server
            String url = "http://" + originServerHost + ":" + originServerPort + "/" + path;
            final byte[] responseData = httpProxyClient.doGet(url);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    cache.writeCache(path, responseData);
                }
            });
            return responseData;
        }
    }
}