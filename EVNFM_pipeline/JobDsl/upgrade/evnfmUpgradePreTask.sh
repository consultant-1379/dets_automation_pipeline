#! /bin/bash
set -eu

## Variables

version=${1}
namespace=${2}

# variable assignment

regUrl=$(kubectl get ingress -n kube-system |grep eric-lcm-container-registry-ingress | awk '{print $3}')
extIngress=$(kubectl get svc -n ingress-nginx | grep LoadBalancer | awk '{print $4}')
hostsUpdate=$(cat /etc/hosts|grep ${regUrl} | awk '{print $2}')


echo "##------------------------- Pre-Check -------------------------##"
echo ""

## Load Balancer IP check

if [[ "${extIngress}" == "<none>" ]]
then
  echo "## WARNING:  ingress-nginx LoadBalacer IP is not properly configured "
  exit 1
else
 echo "## ingress-nginx Load Balacner IP is ${extIngress} "
fi
### Registry check
echo ""
echo "## Registry entry check in /etc/hosts file "

if [[ -z ${hostsUpdate} ]]
then
  echo "## Need to update hosts file with value "
  echo "${extIngress} ${regUrl}" | sudo tee -a /etc/hosts
else
  echo ""
  echo "## Entry is present in /etc/hosts file "
fi


## Making sure eccd have docker
echo "## Making sure docker group added to eccd"
sudo usermod -aG docker eccd
echo ""
echo "##++++++++++++++++++++++++++ Pre-Check ends here ++++++++++++++++++++++++++##"
echo ""

echo "## ----------------------- Pre-Configuration task -----------------------##"
echo ""

## WORKDIR Directory Creation

rm -rf ~/evnfmupgrade
mkdir ~/evnfmupgrade

echo "## WORKDIR Directory created with name 'evnfmupgrade' "

cd ~/evnfmupgrade
# Download the csar file from nexus
echo "## Downloading the file from Nexus "
if wget -q --method=HEAD https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases/com/ericsson/oss/orchestration/eo/eric-eo-evnfm/${version}/eric-eo-evnfm-${version}.csar;
 then
  echo "## This version eo-evnfm csar package present in nexus"
  echo "## DOWNLOADING... "
  wget -q https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases/com/ericsson/oss/orchestration/eo/eric-eo-evnfm/${version}/eric-eo-evnfm-${version}.csar
  sleep 2
  echo "## extracting deployment-manager.tar file"
  unzip -j eric-eo-evnfm-${version}.csar 'Scripts/common/deployment-manager.tar'
  echo "## docker load"
  docker load --input deployment-manager.tar
 else
  echo "## ERROR: The file not present please use correct EO version"
  exit 1
fi

#echo "## Getting eric-eo-evnfm-${version}.csar"
#scp ~/eric-eo-evnfm-${version}.csar ~/evnfmupgrade
#sleep 1
#unzip -j eric-eo-evnfm-${version}.csar 'Scripts/common/deployment-manager.tar'
#echo "## Load the deployment manager"
#docker load --input deployment-manager.tar

## INIT

echo "## INIT task for EVNFM "
echo ""
echo "docker run --rm -u $(id -u):$(id -g) -v ${PWD}:/workdir deployment-manager:${version} init"
echo ""
docker run --rm -u $(id -u):$(id -g) -v ${PWD}:/workdir deployment-manager:${version} init

## Transfer of Kubeconfig file

echo "copying kubeconfig file into workdir"
cp ~/.kube/config kube_config/ || true

# Prepare command

echo "## Site generation docker command "
echo ""
#echo "docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir -v /etc/hosts:/etc/hosts deployment-manager:${version} prepare --namespace ${namespace}"
docker run --rm -u $(id -u):$(id -g) -v ${PWD}:/workdir -v /etc/hosts:/etc/hosts deployment-manager:${version} prepare --namespace ${namespace}

echo "##++++++++++++++++++++++++++ Pre-Configuration task ends here ++++++++++++++++++++++++++##"
echo ""
