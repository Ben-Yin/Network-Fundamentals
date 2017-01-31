import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Map;

/**
 * Created by HappyMole on 9/11/16.
 */
public class HttpClient implements ConstantsAware {

    // do HTTP get request
    public HttpResponse doGet(String url, Map<String, String> headers) {
        HttpResponse httpResponse = doRequest("GET", url, headers, "");
        return httpResponse;
    }

    // do HTTP post request
    public HttpResponse doPost(String url, Map<String, String> headers, String body) {
        HttpResponse httpResponse = doRequest("POST", url, headers, body);
        return httpResponse;
    }

    private HttpResponse doRequest(String method, String url, Map<String, String> headers, String body) {
        String[] s = parseUrl(url);
        String host = s[0];
        int port = Integer.parseInt(s[1]);
        String path = s[2];
        String requestContent = buildRequestContent(method, host, port, path, headers, body);

        Socket socket = null;
        try {
            socket = new Socket(host, port);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
            bufferedOutputStream.write(requestContent.getBytes());
            bufferedOutputStream.flush();

            HttpResponse httpResponse = new ResponseConsumer(socket.getInputStream()).consume();
            return httpResponse;
        } catch (IOException e) {
            System.out.println("Failed to connect to target server!");
            return null;
        }
        finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     *  parseResult[0] = host
     *  parseResult[1] = port
     *  parseResult[2] = path
     */
    private String[] parseUrl(String url) {
        String[] parseResult = new String[3];

        // if the url starts with https, throw the RuntimeException directly
        if (url.startsWith("https://")) {
            throw new RuntimeException("HTTPS is not supported!");
        }

        if (url.startsWith("http://")) {
            url = url.replaceFirst("http://", "");
        }

        String host = url;
        String path = "/";
        String port = "80";
        if (url.indexOf("/") != -1) {
            host = url.substring(0, url.indexOf("/"));
            if (host.indexOf(":") != -1) {
                port = host.substring(host.indexOf(":"));
                host = host.substring(0, host.indexOf(":"));
            }
            path = url.substring(url.indexOf("/"));
        }
        parseResult[0] = host;
        parseResult[1] = port;
        parseResult[2] = path;
        return parseResult;
    }


    private String buildRequestContent(String method, String host, int port, String path, Map<String, String> headers, String body) {
        StringBuilder requestContent = new StringBuilder();

        // add first line
        String firstLine = buildFirstLine(method, path);
        requestContent.append(firstLine);
        requestContent.append(CRLF);

        // add headers content
        String headersContent = buildHeadersContent(headers, host, port, body);
        requestContent.append(headersContent);
        requestContent.append(CRLF);

        // if body is not empty, add body content
        if (body != null && !body.equals("")) {
            requestContent.append(body);
        }

        return requestContent.toString();
    }

    private String buildFirstLine(String method, String path) {
        StringBuilder firstLine = new StringBuilder();
        firstLine.append(method.toUpperCase());
        firstLine.append(" ");
        firstLine.append(path);
        firstLine.append(" ");
        firstLine.append(HTTP_VERSION);
        return firstLine.toString();
    }

    private String buildHeadersContent(Map<String, String> headers, String host, int port, String content) {
        StringBuilder headersContent = new StringBuilder();
        // short connection by default
        headers.put("Connection", "close");

        // add default Host header
        if (port != 80) {
            host = host + ":" + port;
        }
        headers.put("Host", host);

        // add Content-Length header, which is calculated from the length of body content
        if (content != null && content.length() != 0) {
            headers.put("Content-Length", Integer.toString(content.length()));
        }

        for (String key : headers.keySet()) {
            headersContent.append(key + ": " + headers.get(key));
            headersContent.append(CRLF);
        }
        return headersContent.toString();
    }
}
