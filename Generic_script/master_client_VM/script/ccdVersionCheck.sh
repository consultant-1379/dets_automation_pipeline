#! /bin/bash

set -eu

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

clusterID=${1}
masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f3)
SSH_OPTIONS="-o LogLevel=error -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"

# version check
echo "###### CCD version check"
echo ""
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "cat /etc/eccd/eccd_image_version.ini"
echo ""
echo "###### Docker version"
echo ""
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "sudo docker version"
