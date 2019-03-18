mailSync
---
version: 0.1

Preparation
---
Download **activation.jar** and **mail.jar** from this website:

    * https://code.google.com/archive/p/javamail-android/downloads 

Install these two lib locally using Maven:

    * mvn install:install:file -Dfile=/CHANGE_IT/activation.jar -DgroupId=javax.activation -DartifactId=activation -Dversion=10.0.0 -Dpackaging=jar
    * mvn install:install:file -Dfile=/CHANGE_IT/mail.jar -DgroupId=com.sun.mail -DartifactId=javax.mail -Dversion=10.0.0 -Dpackaging=jar

Download **Thunderbird** mail client from this website:

    * https://www.thunderbird.net/en-US/
    
Configure the server settings:

    * Server Name: 127.0.0.1
    * Port: 3143
    * Connection security: None
    * Authentication method: Password, transmitted insecurely
    
Make sure you already followed the instructions on 
(https://bitbucket.org/philoliang/mailsync-anroid/src/ju-test/)
to run the mailSync-android program.

Run the Program
---
Make sure you have connect to the hotspot established by your Android device.

Go to ./greenmail-mailsync/ directory

    . mailsync.sh
    
This script will do the following job: 

1. _Compile the program_
2. _Start the NFD daemon_
3. _Create unicast face and route (now we use default address of Android hotspot: 192.168.43.1:6464, 
some manufactures may change this default address)_
4. _Start the mailSync program_
5. _Ask you to enter your Google account and password_

Run Thunderbird cliend and click the [Get Messages] button to fetch the synced emails.