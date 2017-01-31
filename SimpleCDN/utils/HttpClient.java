package utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by HappyMole on 11/26/16.
 */
public class HttpClient {

    public byte[] doGet(String url) {
        InputStream response = null;

        try {
            response = doGetRequest(url);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(response);
            return ArrayUtils.shiftToBytes(bufferedInputStream);
        } catch (Exception e) {
            CDNLogger.error("failed to get content from the origin server");
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                CDNLogger.error("error happens when closing origin server input stream");
            }
        }
        return null;
    }

    private InputStream doGetRequest(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "CS5700_CDN");
        InputStream inputStream = con.getInputStream();
        return inputStream;
    }
}