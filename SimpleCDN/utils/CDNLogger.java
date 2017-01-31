package utils;

import java.io.IOException;
import java.util.logging.*;

/**
 * Created by HappyMole on 11/28/16.
 */
public class CDNLogger {

    private static Logger logger;

    static {
        logger = Logger.getLogger("CDN logger");
        try {
            Handler fileHandler = new FileHandler("CDN.log");
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void error(String msg) {
        logger.log(Level.SEVERE, msg);
    }
}
