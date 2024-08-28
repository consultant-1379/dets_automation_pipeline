#! /bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

clusterID=${1}

masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f3)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"
internalIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f2)
kubeapi=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f4)
scp ${SSH_OPTIONS} eccd@${masterOamIP}:/home/eccd/.kube/config /home/cenmbuild/AUTO/kubeConfigFile/${clusterID}
#update of kubeapi server
sed -i "s/${internalIP}:6443/${kubeapi}/g" /home/cenmbuild/AUTO/kubeConfigFile/${clusterID}
#source
echo 'export KUBECONFIG=/home/cenmbuild/AUTO/kubeConfigFile/'${clusterID} > /home/cenmbuild/AUTO/sourceFile/${clusterID}
