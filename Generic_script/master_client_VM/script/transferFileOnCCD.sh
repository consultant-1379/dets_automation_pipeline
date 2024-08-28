#! /bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

clusterID=${1}
filepath=${2}
##########
masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f3)
ssh -o LogLevel=error -o 'StrictHostKeyChecking no' eccd@${masterOamIP} kubectl get ns
echo ""
scp -r ${filepath} eccd@${masterOamIP}:/home/eccd || echo "Can't connect to eccdMasterNode";exit 1