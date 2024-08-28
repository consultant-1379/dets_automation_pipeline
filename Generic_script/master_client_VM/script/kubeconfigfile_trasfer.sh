#! /bin/bash

set -eu

clusterID=${1}
kubepath=/home/cenmbuild/AUTO/kubeConfigFile/${clusterID}
registryurl=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f5)

if [[ ! -f ${kubepath} ]]
then
 echo 'Run transfer script'
 bash -x /home/cenmbuild/AUTO/script/kubeconfigCopy.sh ${clusterID}
fi

# transfer of kubeconfig file on minio
scp -o LogLevel=error -o 'StrictHostKeyChecking no' ${kubepath} cenmbuild@214.5.198.4:/home/cenmbuild/.kube/config

# check

ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@214.5.198.4 'kubectl get ns'

## docker login
ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@214.5.198.4 "docker login ${registryurl} -u admin -p dets123"
if [[ $? > 0 ]]
then
echo "check docker login manually resolve any cert or connectivity issue"
exit 1
else
echo "Docker login is ok from this client machine."
fi
