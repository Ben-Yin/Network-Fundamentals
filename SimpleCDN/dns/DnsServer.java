package dns;


import utils.ArrayUtils;
import utils.CDNLogger;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by HappyMole on 11/22/16.
 */

public class DnsServer {
    private static final int MAX_THREADS = 10;

    private String dnsSpecName;

    private MeasureController measureController;
    private DatagramSocket datagramSocket;
    private Executor executor;

    public DnsServer(int port, String dnsSpecName) {
        try {
            // String localIpAddress = NetworkUtils.getLocalIpAddress();
            String localIpAddress = "127.0.0.1";
            InetSocketAddress inetSocketAddress = new InetSocketAddress(localIpAddress, port);
            this.dnsSpecName = dnsSpecName;
            this.measureController = new MeasureController(port);
            this.datagramSocket = new DatagramSocket(inetSocketAddress);
            this.executor = Executors.newFixedThreadPool(MAX_THREADS);
        } catch (Exception e) {
            CDNLogger.error("error happens when initializing DNS server, exit!");
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String dnsSpecName = args[1];

        DnsServer dnsServer = new DnsServer(port, dnsSpecName);
        dnsServer.start();
    }

    public void start() {
        while (true) {
            byte[] buffer = new byte[128];
            final DatagramPacket dnsRequest = new DatagramPacket(buffer, buffer.length);
            try {
                datagramSocket.receive(dnsRequest);
            } catch (IOException e) {
                CDNLogger.error("error happens when receiving dns request");
                continue;
            }
            final InetAddress sourceIpAddr = dnsRequest.getAddress();
            final int sourcePort = dnsRequest.getPort();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    processDnsRequest(dnsRequest, sourceIpAddr, sourcePort);
                }
            };

            executor.execute(runnable);
        }
    }

    private void processDnsRequest(DatagramPacket dnsRequest, InetAddress sourceIpAddr, int sourcePort) {
        String clientIp = dnsRequest.getAddress().getHostAddress();

        int actualLen = dnsRequest.getLength();
        DnsPacket dnsPacket = new DnsPacket(ArrayUtils.truncateArray(dnsRequest.getData(), actualLen));
        if (dnsPacket.getMessageType() == 0 && dnsPacket.getDomainName().equals(dnsSpecName)) {
            // select the best replica server ip for this request, based on the measurements
            String replicaServerIP = measureController.selectBestReplicaServer(clientIp);

            byte[] response = dnsPacket.buildResponse(replicaServerIP);
            DatagramPacket answerPacket = new DatagramPacket(response, response.length, sourceIpAddr, sourcePort);
            try {
                datagramSocket.send(answerPacket);
            } catch (IOException e) {
                CDNLogger.error("error happens when sending dns response");
            }
        }
    }
}
