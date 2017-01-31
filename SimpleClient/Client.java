import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.Security;

/**
 * author: Chi Zhang, Kaibin Yin
 * date: 2016/09/08
 */
public class Client {
    private String NUID;
    private Socket socket;
    private BufferedReader readFromServer;
    private BufferedWriter writeToServer;

    public Client(boolean ssl, String serverName, int port, String nuid) throws IOException {
        if (ssl) {
            Security.addProvider(
                    new com.sun.net.ssl.internal.ssl.Provider());
            System.setProperty("javax.net.ssl.trustStore", "client_ks");
//            System.setProperty("javax.net.debug", "ssl,handshake");
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            this.socket = sslSocketFactory.createSocket(serverName, port);
            ((SSLSocket) this.socket).startHandshake();
        } else {
            this.socket = new Socket(serverName, port);
        }
        this.readFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeToServer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.NUID = nuid;
    }

    public Boolean sendMessageHELLO() {
        /**
         *  return type: Boolean
         *  return sending HELLO message successful or not
         */
        try {
            String hello = "cs5700fall2016 HELLO " + NUID + "\n";
            writeToServer.write(hello);
            writeToServer.flush();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public String getSecretFlag() {
        /**
         *  return type: String
         *  return the 64-BYTE secret flag after the verification of math question
         */
        String respond;
        // before receive the BYE message, sending solutions continuously
        while (true) {
            //  receive the message from server
            try {
                respond = readFromServer.readLine();
            } catch (Exception e) {
                System.out.println("get data from server failed!");
                return null;
            }

            // if the message contains BYE, break the loop
            if (respond.indexOf("BYE") != -1) {
                break;
            }

            //  if the message doesn't contain STATUS,
            //  then the message is invalid
            if (respond.indexOf("STATUS") == -1) {
                System.out.println("receive STATUS from server failed!");
                return null;
            }

            //  calculate the result of expression
            int result = calculateExpression(respond);

            //  send the SOLUTION message to server
            try {
                String solution = "cs5700fall2016 " + String.valueOf(result) + "\n";
                writeToServer.write(solution);
                writeToServer.flush();
            } catch (IOException e) {
                System.out.println("send SOLUTION to server failed!");
                return null;
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("closing socket failed!");
            return null;
        }
        String secretKey = respond.split(" ")[1];
        return secretKey;
    }


    private static int calculateExpression(String str) {
        /**
         *  parameter: the STATUS message received from server
         *  return type: int
         *  return the solution of the STATUS expression
         */
        String exps = str.substring(str.indexOf("STATUS"));
        int num1 = 0;
        int num2 = 0;
        char operator = '.';
        for (int i = 0; i < exps.length(); i++) {
            char curChar = exps.charAt(i);
            if (curChar >= '0' && curChar <= '9') {
                if (operator == '.') {
                    num1 *= 10;
                    num1 += curChar - '0';
                } else {
                    num2 *= 10;
                    num2 += curChar - '0';
                }
            } else if (curChar == '+' || curChar == '-' || curChar == '*' || curChar == '/') {
                operator = curChar;
            }
        }
        switch (operator) {
            case '+':
                return num1 + num2;
            case '-':
                return num1 - num2;
            case '*':
                return num1 * num2;
            case '/':
                return num1 / num2;
            default:
                return -1;
        }
    }
}
