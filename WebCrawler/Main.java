import java.util.Set;

/**
 * Created by BenYin on 9/22/16.
 */
public class Main {
    public static void main(String[] args){
        String userName = args[0];
        String password = args[1];
//        String userName = "001221610";
//        String password = "HMHB1D36";

        // init the crawler
        WebCrawler crawler = new WebCrawler();
        // login with the given user name and password
        String cookie = crawler.login(userName, password);
        // if the login cookie is null, exit the program
        if (cookie == null) {
            System.out.println("Failed to login fakebook!");
            System.exit(-1);
        }

        // start crawling
        crawler.start(cookie);

        // print all secret flags
        Set<String> secretFlags = crawler.getSecretFlags();
        for (String secretFlag : secretFlags) {
            System.out.println(secretFlag);
        }

        System.exit(0);
    }
}
