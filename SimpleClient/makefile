# default to compile
default: Main.class Client.class to_executable 

Main.class: Main.java
	javac Main.java

Client.class: Client.java
	javac Client.java

to_executable:
	chmod +x client

# Use 'make clean' to remove all compiled java classes
clean:
	$(RM) *.class
