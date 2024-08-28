#! /bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi


clusterID=${1}
PODNAME=${1}cenm


if [[ ! -f "/home/cenmbuild/AUTO/kubeConfigFile/${clusterID}" ]]
then
  bash -x /home/cenmbuild/AUTO/script/kubeconfigCopy.sh ${clusterID}
fi

echo "Getting the namespace"
namespace=$(kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${clusterID} get ns| grep ${PODNAME} | awk '{print $1}')

echo "====== ${clusterID} - cENM version ======"
kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${clusterID} exec deploy/troubleshooting-utils -n ${namespace} -- enm_version
echo ""

echo "====== ${clusterID} - cENM HC -- long ======"
kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${clusterID} exec deploy/troubleshooting-utils -n ${namespace} -- enm_hc -v
echo ""

echo "====== ${clusterID} - cENM HC -- short ======"
kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${clusterID} exec deploy/troubleshooting-utils -n ${namespace} -- enm_hc
echo ""
