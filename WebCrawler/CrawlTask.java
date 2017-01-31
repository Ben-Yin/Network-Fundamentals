import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Created by HappyMole on 9/23/16.
 */
public class CrawlTask implements Runnable, ConstantsAware {
    private String url;
    // headers for the request
    private Map<String, String> headers;
    // urls that have been visited
    private Set<String> visitedURL;
    // secret flags
    private Set<String> secretFlags;
    // url that will be accessed
    private Queue<String> newURLQueue;

    public CrawlTask(String url, Map<String, String> headers, Set<String> visitedURL, Queue<String> newURLQueue, Set<String> secretFlags) {
        this.url = url;
        this.headers = headers;
        this.visitedURL = visitedURL;
        this.secretFlags = secretFlags;
        this.newURLQueue = newURLQueue;
    }

    @Override
    public void run() {
//        System.out.println(HOME_URL + url);
        HttpResponse response = httpClient.doGet(HOME_URL + url, headers);
        int statusCode = response.getStatusCode();

        // if statusCode is 403 or 404, abandon the request
        if (statusCode == 403 || statusCode == 404) {
            return;
        }
        // if statusCode is 301, try the request again using the new URL given by the server
        if (statusCode == 301) {
            String location = response.getHeader("Location").get(0).replace("Location:", "").trim();
            response = httpClient.doGet(HOME_URL + location, headers);
            statusCode = response.getStatusCode();
        }
        // if statusCode is 500, retry the request
        int i = 0;
        if (statusCode == 500) {
            while (response.getStatusCode() == 500 && i++ < MAX_RETRY_TIMES) {
                response = httpClient.doGet(HOME_URL + url, headers);
            }
        }
        // if statusCode is 200, mark the url as visited and crawl its content
        if (response.getStatusCode() == 200 && response.getMessage().equals("OK")) {
            synchronized (visitedURL) {
                visitedURL.add(url);
            }
            addContentURLsToQueue(response.getContent());
            searchContentSecretFlag(response.getContent());
        }
    }

    // search and add urls in this page to the queue
    private void addContentURLsToQueue(String content) {
        Matcher linkMatcher = linkPattern.matcher(content);
        while (linkMatcher.find()) {
            String link = linkMatcher.group(1).trim();
            synchronized (newURLQueue) {
                newURLQueue.add(link);
            }
        }
    }

    // search and add secret flags in this page to the queue
    private void searchContentSecretFlag(String content) {
        Matcher flagMatcher = flagPattern.matcher(content);
        while (flagMatcher.find()) {
            String flag = flagMatcher.group(1).trim();
            if (flag.length() == 64) {
                synchronized (secretFlags) {
                    secretFlags.add(flag);
                }
            }
        }
    }
}
