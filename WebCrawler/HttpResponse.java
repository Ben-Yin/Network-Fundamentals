import java.util.ArrayList;
import java.util.List;

/**
 * Created by HappyMole on 9/11/16.
 */
public class HttpResponse {
    private String version;
    private int statusCode;
    private String message;
    private List<String> headers;
    private String content;

    public HttpResponse(String version, int statusCode, String message, List<String> headers, String content) {
        this.version = version;
        this.statusCode = statusCode;
        this.message = message;
        this.headers = headers;
        this.content = content;
    }

    public String getVersion() {
        return version;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getContent() {
        return content;
    }

    // since sometimes there will be headers with the same name, so here we should return a list of header
    public List<String> getHeader(String headerName) {
        List<String> headerList = new ArrayList<String>();
        for (String header : headers) {
            String name = header.substring(0, header.indexOf(":")).trim();
            if (name.equals(headerName)) {
                headerList.add(header);
            }
        }
        return headerList;
    }
}