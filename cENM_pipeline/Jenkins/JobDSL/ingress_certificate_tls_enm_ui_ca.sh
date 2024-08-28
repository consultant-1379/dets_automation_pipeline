#! /bin/bash

set -eu

clustrID=${1}
namespace=${2}

masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/vpodMasterOamIp | grep ${clustrID} |cut -d "=" -f2)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"

## Run command remotely

ssh ${SSH_OPTIONS} eccd@${masterOamIP} "bash -s ${namespace}" < ./home/cenmbuild/AUTO/script/ingressScript.sh



#############


#! /bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

set -eu

##Variable

clustrID=${1}
namespace=${2}

masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/vpodMasterOamIp | grep ${clustrID} |cut -d "=" -f2)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 -o LogLevel=error"

########## TransferScript

ssh ${SSH_OPTIONS} eccd@${masterOamIP} kubectl get ns
echo ""
scp -r /home/cenmbuild/cENM/Scripts/ingress_certificate_tls_enm_ui_ca/ eccd@${masterOamIP}:/home/eccd || echo "Can't connect to eccdMasterNode"

## Run command remotely
cd /home/cenmbuild/AUTO/script/ 
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "bash -s ${namespace}" < ./ingressScript.sh
