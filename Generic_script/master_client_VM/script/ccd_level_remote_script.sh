#! /bin/bash

#set -eu

##Variable

clusterID=${1}
pem_file=${2}
vpodNumber=$(echo ${clusterID} | cut -d 'p' -f2)
IronicIP=$(cat Generic_script/master_client_VM/script/bin/ironicIP | awk '(NR=='${vpodNumber}')' | cut -d "=" -f2)
masterOamIP=$(cat inventory.csv | grep ${clusterID} | cut -d "," -f3)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 -o LogLevel=error"


## Run command remotely
ssh -i ${pem_file} ${SSH_OPTIONS} eccd@${masterOamIP} "bash -s ${IronicIP}" < ./Generic_script/master_client_VM/script/ccd_install_post_HC.sh

sleep 1
## transfer of ceph script

ssh -i ${pem_file} ${SSH_OPTIONS} eccd@${masterOamIP} 'kubectl get ns'

scp -i ${pem_file} -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no Generic_script/master_client_VM/script/ceph_status_check.sh eccd@${masterOamIP}:/home/eccd/

ssh -i ${pem_file} ${SSH_OPTIONS} eccd@${masterOamIP} 'bash /home/eccd/ceph_status_check.sh'