#! /bin/bash

set -eu

version=${1}
namespace=${2}
deployment_type=${3}
helm_timeout=${4}

if [ ${deployment_type} == "install" ]
then
  cd ~/evnfmdeploy
else
  cd ~/evnfmupgrade
fi

echo "##--------------------------- evnfm ${deployment_type} started ---------------------------##"
echo ""
echo "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:/workdir -v /etc/hosts:/etc/hosts deployment-manager:${version} ${deployment_type} --namespace ${namespace} --crd-namespace crd --iccr-crd-helm-release-name eric-tm-ingress-controller-cr-crd &"

docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:/workdir -v /etc/hosts:/etc/hosts deployment-manager:${version} ${deployment_type} --helm-timeout ${helm_timeout} --namespace ${namespace} --crd-namespace crd --iccr-crd-helm-release-name eric-tm-ingress-controller-cr-crd &