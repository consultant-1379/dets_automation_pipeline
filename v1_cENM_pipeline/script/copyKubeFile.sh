#! /bin/bash

set -eu

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

InventoryPath="$(dirname $0)/bin/inventory.csv"
HomeDir=$(pwd ../../../$(dirname $0))
echo $InventoryPath
echo $HomeDir
clusterID=${1}
pemFile=${2}

masterOamIP=$(cat ${InventoryPath} | grep ${clusterID} | cut -d "," -f3)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"
internalIP=$(cat ${InventoryPath} | grep ${clusterID} | cut -d "," -f2)
kubeapi=$(cat ${InventoryPath} | grep ${clusterID} | cut -d "," -f4)
echo $masterOamIP
echo $internalIP
echo $kubeapi
#Directory create
rm -rf ${HomeDir}/org_kube_file
mkdir -p ${HomeDir}/org_kube_file
if [[ -d "${HomeDir}/org_kube_file" ]]
then
    scp -i ${pemFile} ${SSH_OPTIONS} eccd@${masterOamIP}:/home/eccd/.kube/config ${HomeDir}/org_kube_file/${clusterID}_kubeFile
    #update of kubeapi server
    if [[ $internalIP == "0.0.0.0" ]]
    then
      echo "Internal IP not provided in inventory.csv file. Will fetch internal IP from existing kubeconfig and then replace with External FQDN"
      internalIpVal=$(grep -oP 'server: https://\K.+' "${HomeDir}/org_kube_file/${clusterID}_kubeFile")
      sed -i "s/${internalIpVal}/${kubeapi}/g" ${HomeDir}/org_kube_file/${clusterID}_kubeFile
    else
      echo "Internal IP provided in inventory.csv file. Will replace it with External FQDN"
      sed -i "s/${internalIP}:6443/${kubeapi}/g" ${HomeDir}/org_kube_file/${clusterID}_kubeFile
    fi
else
    logger "Directory not found where kube config file supposed to be stored"
    exit 3
fi