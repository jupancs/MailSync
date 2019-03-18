#!/bin/bash

cd ..
nfd-start >/dev/null 2>&1 &
sleep 1
mvn compile
STATUS=$?
if [[ $STATUS -ne 0 ]] ; then
	echo 'Cannot compile this program!'; exit $rc
fi

#nfdc face create udp4://192.168.43.1:6363 && nfdc route add /mailSync udp4://192.168.43.1:6363 && 
cd greenmail-core

mvn exec:java -Dexec.mainClass="com.icegreen.greenmail.ExternalProxy" && cd ..

