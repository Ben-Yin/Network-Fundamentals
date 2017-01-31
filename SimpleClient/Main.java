import java.io.*;

public class Main {

    public static void main(String[] args) throws IOException {
        /**
         *  Author: Kaibin Yin
         *  date:   2016-09-08
         *  args:   port (int), useSSL (Boolean), serverName (String), NUID (int)
         */
        int port = Integer.parseInt(args[0]);
        boolean useSSL = Boolean.parseBoolean(args[1]);
        String serverName = args[2];
        String NUID = args[3];
        Client client = null;
        /*
        System.out.println("------------------------------------------------");
        System.out.println("port    useSSL       servername         NUID");
        System.out.println(port+"   "+useSSL+"    "+serverName+"   "+NUID);
        System.out.println("------------------------------------------------");
        */
        //  using TCP socket to build connection
        try {
            client = new Client(useSSL, serverName, port, NUID);
        } catch (IOException e) {
            System.out.println("Fail to create the client socket!");
            System.exit(-1);
        }

        //regular connection
        if (!client.sendMessageHELLO()) {
            System.out.println("Fail to send HELLO to the server!");
            System.exit(-1);
        }

        String secretFlag = client.getSecretFlag();
        // if error happens when trying to get the secret flag
        if (secretFlag == null) {
            System.exit(-1);
        }

        // if the husky id is unkown
        if (secretFlag.indexOf("Unknown_Husky_ID") != -1) {
            System.out.println("Invalid NUID!");
            System.exit(-1);
        }

        // if the secret flag has an erroneous format
        if (secretFlag.length() != 64) {
            System.out.println("Invalid secret key format!");
            System.exit(-1);
        } else {
            System.out.println(secretFlag);
        }
        //  save secret flag to local file: "secret_flag"
        /*File flagFile = new File("./secret_flag");
        FileWriter writeToFile = new FileWriter(flagFile, true);
        if (!flagFile.exists()){
            flagFile.createNewFile();
        }
        writeToFile.write(secretFlag+"\n");
        writeToFile.flush();
        writeToFile.close();*/
    }
}
