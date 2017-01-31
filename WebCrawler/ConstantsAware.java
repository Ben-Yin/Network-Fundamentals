import java.util.regex.Pattern;

/**
 * Created by HappyMole on 9/23/16.
 */
public interface ConstantsAware {
    // the default http version should be HTTP/1.0
    String HTTP_VERSION = "HTTP/1.0";
    // the default characters for the new line
    String CRLF = "\r\n";
    String LOGIN_URL = "http://cs5700f16.ccs.neu.edu/accounts/login/?next=/fakebook/";
    String HOME_URL = "http://cs5700f16.ccs.neu.edu/fakebook/";
    // max retry times for the page that returns 501
    int MAX_RETRY_TIMES = 5;
    // size of the thread pool for crawl task
    int NUMBER_OF_THREADS = 20;
    // the regex pattern for searching links
    Pattern linkPattern = Pattern.compile("<a href=\"/fakebook/(.*?)\">");
    // the regex pattern for searching secret flags
    Pattern flagPattern = Pattern.compile("<h2 class='secret_flag' style=\"color:red\">FLAG:(.+?)</h2>");
    HttpClient httpClient = new HttpClient();
}
