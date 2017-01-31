HTTP_JFLAGS = -cp .:jar/json-simple-1.1.1.jar
DNS_JFLAGS = -cp .:jar_dns/async-http-client-1.9.40.jar:jar/json-simple-1.1.1.jar:jar_dns/slf4j-api-1.7.21.jar:jar_dns/slf4j-simple-1.7.21.jar
JC = javac


default: DNS_CLASSES HTTP_CLASSES DNS_EXECUTABLE HTTP_EXECUTABLE DEPLOY_EXECUTABLE

dns: DNS_CLASSES DNS_EXECUTABLE

http: HTTP_CLASSES HTTP_EXECUTABLE

deploy: DEPLOY_EXECUTABLE

DNS_CLASSES: dns/DnsServer.java
	$(JC) $(DNS_JFLAGS) dns/DnsServer.java

DNS_EXECUTABLE:
	chmod +x dnsserver

HTTP_CLASSES: http/HttpServer.java
	$(JC) $(HTTP_JFLAGS) http/HttpServer.java

HTTP_EXECUTABLE:
	chmod +x httpserver

DEPLOY_EXECUTABLE:
	chmod +x deployCDN
	chmod +x runCDN
	chmod +x stopCDN


clean:
	$(RM) dns/*class http/*class utils/*class
