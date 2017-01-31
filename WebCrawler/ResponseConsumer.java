import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by HappyMole on 9/21/16.
 */
public class ResponseConsumer {
    private InputStream inputStream;

    public ResponseConsumer(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public HttpResponse consume() {
        // read the first line and all headers
        List<String> firstLineAndHeaders = consumeFirstLineAndHeaders();

        String firstLine = null;
        List<String> headerList = new ArrayList<String>();
        String contentEncoding = null;
        String transferEncoding = null;
        // split headers between the first line, and parse useful headers for reading other contents
        if (firstLineAndHeaders != null && firstLineAndHeaders.size() > 0) {
            firstLine = firstLineAndHeaders.get(0);
            for (int i = 1; i < firstLineAndHeaders.size(); i++) {
                String headerLine = firstLineAndHeaders.get(i);
                int splitIndex = headerLine.indexOf(":");
                String headerName = headerLine.substring(0, splitIndex).trim();
                String headerValue = headerLine.substring(splitIndex + 1).trim();
                if (headerName.equals("Content-Encoding")) {
                    contentEncoding = headerValue;
                } else if (headerName.equals("Transfer-Encoding")) {
                    transferEncoding = headerValue;
                }

                headerList.add(headerLine);
            }
        }

        String responseContent = consumeContent(contentEncoding, transferEncoding);

        HttpResponse httpResponse = buildHttpResponse(firstLine, headerList, responseContent);
        return httpResponse;
    }

    // read first line and headers from the input
    private List<String> consumeFirstLineAndHeaders() {
        List<String> firstLineAndHeaders = new ArrayList<String>();
        int n;
        int start = 0;
        byte[] buffer = new byte[4092];
        try {
            while ((n = inputStream.read()) != -1) {
                buffer[start++] = (byte) n;
                // if met CRLF, record the current line
                if (n == '\r' && inputStream.read() == '\n') {
                    String line = new String(buffer).trim();
                    // current line being empty means all headers have been read
                    if (line.equals("")) {
                        break;
                    }
                    // add the line to the list
                    firstLineAndHeaders.add(line);
                    buffer = new byte[4092];
                    start = 0;
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Fail to consume header from website!");
            System.out.println(e);
        }
        return firstLineAndHeaders;
    }

    // currently do not support "Transfer-Encoding=chunked"
    private String consumeContent(String contentEncoding, String transferEncoding) {
        try {
            // if the Content-Encoding is gzip, use GZIPInputStream to wrap the inputStream
            if (contentEncoding != null && contentEncoding.equals("gzip")) {
                inputStream = new GZIPInputStream(inputStream);
            }

            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[256];
            int bufferSize;

            while ((bufferSize = inputStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bufferSize));
            }
            return sb.toString();
        } catch (IOException e) {
            System.err.println("Fail to consume content from website!");
        }

        return "";
    }


    // build HttpResponse object with first line, headers and content
    private HttpResponse buildHttpResponse(String firstLine, List<String> headers, String content) {
        if (firstLine != null && !firstLine.equals("")) {
            String[] strArr = firstLine.split(" ");
            String version = strArr[0];
            int statusCode = Integer.parseInt(strArr[1]);
            String message = strArr[2];
            HttpResponse httpResponse = new HttpResponse(version, statusCode, message, headers, content);
            return httpResponse;
        }

        return null;
    }
}
