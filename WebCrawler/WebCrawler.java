//kaibin yin at 9/21/16 6:29 PM

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class WebCrawler implements ConstantsAware {
    private Queue<String> newURLQueue;
    private Set<String> visitedURL;
    private Set<String> secretFlags;


    public WebCrawler() {
        this.newURLQueue = new LinkedBlockingQueue<String>();
        this.visitedURL = Collections.synchronizedSet(new HashSet<String>());
        this.secretFlags = Collections.synchronizedSet(new HashSet<String>());
    }

    public String login(String userName, String password) {
        String csrf = getCsrf();
        // if csrf is null, return null directly
        if (csrf == null) {
            return null;
        }

        // build headers for the login request
        Map<String, String> headers = new HashMap<String, String>();
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append("csrftoken=");
        cookieBuilder.append(csrf);
        cookieBuilder.append("; ");
        headers.put("Cookie", cookieBuilder.toString());

        // build post data for the login request
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("username=");
        formBuilder.append(userName);
        formBuilder.append("&password=");
        formBuilder.append(password);
        formBuilder.append("&csrfmiddlewaretoken=");
        formBuilder.append(csrf);
        formBuilder.append("&next=%2Ffakebook%2F");

        // do login
        HttpResponse httpResponse = httpClient.doPost(LOGIN_URL, headers, formBuilder.toString());
        // 302 status code means the login is successful
        if (httpResponse.getStatusCode() == 302) {
            newURLQueue.offer("");
            String cookieHeader = httpResponse.getHeader("Set-Cookie").get(0);
            String cookie = cookieHeader.substring(cookieHeader.indexOf(":") + 1);
            return cookie;
        } else {
            return null;
        }
    }

    // open the login url with the HTTP get method, and parse crsf token from the response headers
    private String getCsrf() {
        HttpResponse response = httpClient.doGet(LOGIN_URL, new HashMap<String, String>());
        if (response == null) {
            return null;
        }

        for (String setCookieHeader : response.getHeader("Set-Cookie")) {
            if (setCookieHeader.contains("csrftoken")) {
                String csrf = setCookieHeader.substring(setCookieHeader.indexOf("csrftoken="), setCookieHeader.indexOf(";"));
                csrf = csrf.replace("csrftoken=", "");
                return csrf;
            }
        }
        return null;
    }

    // start crawling the pages
    public void start(String cookie) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", cookie);
        // create a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        // search for the secret flags until all 5 of them are found
        do {
            if (!newURLQueue.isEmpty()) {
                String url = newURLQueue.poll();
                synchronized (visitedURL) {
                    if (!visitedURL.contains(url)) {
                        executorService.execute(new CrawlTask(url, headers, visitedURL, newURLQueue, secretFlags));
                        visitedURL.add(url);
                    }
                }
            }
        } while (secretFlags.size() < 5);
    }

    public Set<String> getSecretFlags() {
        return secretFlags;
    }
}
