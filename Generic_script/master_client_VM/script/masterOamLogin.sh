#! /bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

clusterID=${1}

##########
masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f3)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} || echo "Can't connect to eccdMasterNode";exit 1
