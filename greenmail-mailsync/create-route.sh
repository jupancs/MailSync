#!/bin/bash
read -p 'IP Address: ' ipaddr
nfdc face create udp4://$ipaddr
nfdc route add /mailSync udp4://$ipaddr
