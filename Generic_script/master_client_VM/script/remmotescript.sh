#! /bin/bash

set -eu

##Variable

clusterID=${1}
namespace=${2}

masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f3)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 -o LogLevel=error"

########## TransferScript

ssh ${SSH_OPTIONS} eccd@${masterOamIP} kubectl get ns
echo ""
scp ${SSH_OPTIONS} -r /home/cenmbuild/cENM/Scripts/ingress_certificate_tls_enm_ui_ca/ eccd@${masterOamIP}:/home/eccd || echo "Can't connect to eccdMasterNode"

## Run command remotely
cd /home/cenmbuild/AUTO/script/ 
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "bash -s ${namespace}" < ./ingressScript.sh

