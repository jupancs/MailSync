#!/bin/bash
read -p 'IP Address: ' ipaddr
nfdc route remove /mailSync udp4://$ipaddr
