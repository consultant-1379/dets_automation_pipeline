#! /bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

clusterID=${1}
pathToStoreCert='/home/cenmbuild/AUTO/kubeCert/'
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"

# directory creation in master cvm
for i in $(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep -v clusterid| cut -d "," -f5)
do 
  if [[ ! -d ${pathToStoreCert}${i} ]]
  then
    mkdir -p ${pathToStoreCert}${i}
  else
    echo "Directory already exist"
  fi
done

# Copy cert to each dir

if [[ ${clusterID} == 'all' ]]
then
  for ID in $(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep -v clusterid|cut -d "," -f1)
  do
    masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${ID}|cut -d "," -f3)
    registryUrl=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${ID}| cut -d "," -f5)
    output=$(ssh -oBatchMode=yes -o LogLevel=error -o 'StrictHostKeyChecking no' -o ConnectTimeout=5 eccd@${masterOamIP} echo > /dev/null 2>&1 && echo "Success" || echo "No Connection")
    if [[ ${output} == 'Success' ]]
    then 
      ssh ${SSH_OPTIONS} eccd@${masterOamIP} 'sudo scp /etc/kubernetes/pki/ca.crt /home/eccd/ca.crt'
      ssh ${SSH_OPTIONS} eccd@${masterOamIP} 'sudo chown eccd:eccd /home/eccd/ca.crt'
      scp ${SSH_OPTIONS} eccd@${masterOamIP}:/home/eccd/ca.crt ${pathToStoreCert}${registryUrl}
    else
      echo "IP: ${masterOamIP} not ok from this mcvm"
    fi
  done
else
  masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f3)
  registryUrl=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f5)
  output=$(ssh -oBatchMode=yes -o LogLevel=error -o 'StrictHostKeyChecking no' -o ConnectTimeout=5 eccd@${masterOamIP} echo > /dev/null 2>&1 && echo "Success" || echo "No Connection")
  if [[ ${output} == 'Success' ]]
  then
    ssh ${SSH_OPTIONS} eccd@${masterOamIP} 'sudo scp /etc/kubernetes/pki/ca.crt /home/eccd/ca.crt'
    ssh ${SSH_OPTIONS} eccd@${masterOamIP} 'sudo chown eccd:eccd /home/eccd/ca.crt'
    scp ${SSH_OPTIONS} eccd@${masterOamIP}:/home/eccd/ca.crt ${pathToStoreCert}${registryUrl}
  else
    echo "IP: ${masterOamIP} not ok from this mcvm"
  fi
fi


