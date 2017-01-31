# default to compile
default: Main.class HttpClient.class HttpResponse.class WebCrawler.class ResponseConsumer.class CrawlTask.class to_executable 

Main.class: Main.java
	javac Main.java

HttpClient.class: HttpClient.java
	javac HttpClient.java

HttpResponse.class: HttpResponse.java
	javac HttpResponse.java

WebCrawler.class: WebCrawler.java
	javac WebCrawler.java

ResponseConsumer.class: ResponseConsumer.java
	javac ResponseConsumer.java

CrawlTask.class: CrawlTask.java
	javac CrawlTask.java

to_executable:
	chmod +x webcrawler

# Use 'make clean' to remove all compiled java classes
clean:
	$(RM) *.class
