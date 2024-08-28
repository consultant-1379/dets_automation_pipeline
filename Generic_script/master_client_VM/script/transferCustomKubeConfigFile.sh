#! /bin/bash
set -eu

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

clusterID=${1}

##########
ccdClientIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f6)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"
sshpass -p 'Mydgurq6gaYO2f6A6Il' scp ${SSH_OPTIONS} /home/cenmbuild/AUTO/storeFile/RBAC/${clusterID}_custom_kubeconfigFile eccd@${ccdClientIP}:/home/eccd/.kube/config || echo "Can't connect to eccdMasterNode";exit 1


