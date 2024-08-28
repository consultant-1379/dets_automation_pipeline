#! /bin/bash
set -eu

## Variables

EO_version=$1
evnfm_namespace=$2
evnfmdeploydir=${3}

echo "EO_version=$1" > /tmp/Variables
echo "evnfm_namespace=$2" >> /tmp/Variables
#version=$(cat /var/tmp/version | grep 1.35.0-177)
#namespace=$(cat /var/tmp/namespace | grep evnfm1)

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

echo "## ----------------------- Pre-Configuration task -----------------------##"
echo ""

## WORKDIR Directory Creation
rm -rf ${evnfmdeploydir}
mkdir ${evnfmdeploydir}
echo "## WORKDIR Directory created with name 'evnfmdeploy' "
sleep 2
cd ${evnfmdeploydir}
## Download the csar file from nexus
source /tmp/Variables
echo "## Downloading the file from Nexus "
echo "Loking for $EO_version"
if wget -q --method=HEAD https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases/com/ericsson/oss/orchestration/eo/eric-eo-evnfm/${EO_version}/eric-eo-evnfm-${EO_version}.csar
 then
  echo "## This version eo-evnfm csar package present in nexus"
  echo "## DOWNLOADING... "
  wget -q https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases/com/ericsson/oss/orchestration/eo/eric-eo-evnfm/${EO_version}/eric-eo-evnfm-${EO_version}.csar
  sleep 2
  echo "## extracting deployment-manager.tar file"
  echo $PWD
  echo "eric-eo-evnfm-${EO_version}.csar"
  unzip -j eric-eo-evnfm-${EO_version}.csar 'Scripts/common/deployment-manager.tar'
  echo "## docker load"
  docker load --input deployment-manager.tar
 else
  echo "## ERROR: The file not present please use correct EO version"
  exit 1
fi
#echo "## Getting eric-eo-evnfm-${EO_version}.csar"
#scp ~/eric-eo-evnfm-${EO_version}.csar ~/evnfmdeploy
#sleep 1
#unzip -j eric-eo-evnfm-${EO_version}.csar 'Scripts/common/deployment-manager.tar'
#echo "## Load the deployment manager"

## INIT

echo "## INIT task for EVNFM "
echo ""
echo "docker run --rm -u $(id -u):$(id -g) -v ${PWD}:/workdir deployment-manager:${EO_version} init"
echo ""
docker run --rm -u $(id -u):$(id -g) -v ${PWD}:/workdir deployment-manager:${EO_version} init

## Transfer of Kubeconfig file

echo "copying kubeconfig file into workdir"
cp ~/.kube/config kube_config/ || true

## Namespace creation

existingnamespaceCheck=$(kubectl get ns | grep ${evnfm_namespace}| awk '{print $1}')
if [[ -z "${existingnamespaceCheck}" ]]
then
  echo "## creating ${evnfm_namespace} namespcae"
  kubectl create ns ${evnfm_namespace}
else
  echo "## ${evnfm_namespace} already present..! DID you clean down the evnfm before running evnfm deploy?"
  exit 1
fi

existingnamespaceCheckCRD=$(kubectl get ns | grep crd| awk '{print $1}')
if [[ -z "${existingnamespaceCheckCRD}" ]]
then
  echo "creating crd namespcae"
  kubectl create ns crd
else
  echo "crd namespace already present..!"
fi

# Prepare command

echo "## Site generation docker command "
echo ""
#echo "docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir -v /etc/hosts:/etc/hosts deployment-manager:${EO_version} prepare --namespace ${evnfm_namespace}"
docker run --rm -u $(id -u):$(id -g) -v ${PWD}:/workdir -v /etc/hosts:/etc/hosts deployment-manager:${EO_version} prepare --namespace ${evnfm_namespace}

#### Create Service Account file####

echo "apiVersion: v1
kind: ServiceAccount
metadata:
  name: evnfm
automountServiceAccountToken: true" > ServiceAccount.yaml

#### Create Cluster Role Binding file####

echo "apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ${evnfm_namespace}
subjects:
  - kind: ServiceAccount
    # Reference to ServiceAccount metadata.name
    name: evnfm
    namespace: ${evnfm_namespace}
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io" > ClusterRoleBinding.yaml

#### Create htpasswd file for evnfm-user ####

echo "## Create htpasswd file "
/usr/bin/htpasswd -cBb htpasswd vnfm-user DefaultP12345!


#### Create service account, cluster role and secrets ####

echo "## create service account "
kubectl create -f ServiceAccount.yaml --namespace ${evnfm_namespace}
sleep 2

echo "## create cluster role binding "
kubectl create -f ClusterRoleBinding.yaml
sleep 2

echo "##create secret using htpasswd file "
kubectl create secret generic container-registry-users-secret --from-file=htpasswd=./htpasswd --namespace ${evnfm_namespace}
sleep 2

echo "##create secret for IAM admin user "
kubectl create secret generic eric-sec-access-mgmt-creds --from-literal=kcadminid=admin --from-literal=kcpasswd=test --from-literal=pguserid=admin --from-literal=pgpasswd=test-pw --namespace ${evnfm_namespace}
sleep 2

echo "##create secret for postgres admin user "
kubectl create secret generic eric-eo-database-pg-secret --from-literal=custom-user=eo_user --from-literal=custom-pwd=postgres --from-literal=super-user=postgres --from-literal=super-pwd=postgres --from-literal=metrics-user=exporter --from-literal=metrics-pwd=postgres --from-literal=replica-user=replica --from-literal=replica-pwd=postgres --namespace ${evnfm_namespace}
sleep 2

echo "##++++++++++++++++++++++++++ Pre-Configuration task ends here ++++++++++++++++++++++++++##"
echo ""
