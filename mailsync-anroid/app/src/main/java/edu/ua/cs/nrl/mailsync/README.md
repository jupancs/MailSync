Preparation
---
Download **activation.jar** and **mail.jar** from this website:

    * https://code.google.com/archive/p/javamail-android/downloads 

Install these two lib locally using Maven:

    * mvn install:install:file -Dfile=/CHANGE_IT/activation.jar -DgroupId=javax.activation -DartifactId=activation -Dversion=10.0.0 -Dpackaging=jar
    * mvn install:install:file -Dfile=/CHANGE_IT/mail.jar -DgroupId=com.sun.mail -DartifactId=javax.mail -Dversion=10.0.0 -Dpackaging=jar

Download **Thunderbird** mail client from this website:

    * https://www.thunderbird.net/en-US/

Use the testing email below to log in:
    
    * Email: emailsynctest1@gmail.com
    * Password: emailsync
    
Configure the server settings:

    * Server Name: 127.0.0.1
    * Port: 3143
    * Connection security: None
    * Authentication method: Password, transmitted insecurely
    
Start NFD Daemon:

    * nfd-start

Multicast Test (2 laptops)
---
**(For each machine)**

Check out all the face

    * nfdc face
   
In the result, look for something like this, remember the **faceid** here:
   
    faceid=261 remote=udp4://224.0.23.170:56363 local=udp4://192.168.0.12:51737 congestion={base-marking-interval=100ms default-threshold=65536B} counters={in={0i 0d 0n 0B} out={0i 0d 0n 0B}} flags={non-local permanent multi-access congestion-marking}
    
Add route:

    * nfdc route add /mailSync 261

Go to /greenmail-mailsync directory

    * mvn compile    
    
Go to /greenmail-mailsync/greenmail-core directory

    * mvn exec:java -Dexec.mainClass="com.icegreen.greenmail.ExternalProxy"

Unicast Test (2 laptops)
---
Check out each machine's ip address _**ip1**_ and **_ip2_**

On machine1:

    * nfdc face creat udp4://<ip2>:6363
    
On machine2:

    * nfdc face creat udp4://<ip1>:6363
    
Then look up the faceid for both machine

    * nfdc face

On each machine:

    * nfdc route add /mailSync faceid
    
Go to /greenmail-mailsync directory

    * mvn compile    
    
Go to /greenmail-mailsync/greenmail-core directory

    * mvn exec:java -Dexec.mainClass="com.icegreen.greenmail.ExternalProxy"
    
Test
---
After done with either of **Multicast Test** or **Unicast Test** above

On one of the machine, in Thunderbird, click **Get Messages**, you should see the 
client received some emails. But the email body is "Errors". No worries, this 
will be solved later.

<!--java -cp target/greenmail-1.6.0-SNAPSHOT.jar com.icegreen.greenmail.ExternalProxy -->

