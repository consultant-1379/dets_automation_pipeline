#! /bin/bash

evnfmnamespace=${1}
crdnamespace=${2}
helm="/usr/local/bin/helm"

## Uninstall EO helm
echo "uninstall eric-eo-${evnfmnamespace}"
${helm} uninstall eric-eo-${evnfmnamespace} --namespace ${evnfmnamespace}

##  pvc
echo "delete pvc"
kubectl delete pvc --all --namespace ${evnfmnamespace}

## pv
echo "check any PV"
kubectl get pv | grep ${evnfmnamespace}

## namespace delete
echo "delete namespace"
kubectl delete namespace ${evnfmnamespace}

## check
echo "Final check"
kubectl get all --namespace ${evnfmnamespace}
kubectl get configmaps --namespace ${evnfmnamespace}

## crd helm uninstall

helm -n ${crdnamespace} uninstall eric-tm-ingress-controller-cr-crd

## remove from crd

kubectl delete crd --namespace ${crdnamespace} $(kubectl get crd --namespace ${crdnamespace} | grep projectcontour | cut -f1 -d' ')

## ClusterRoleBinding
echo "cluster role binding"
kubectl delete ClusterRoleBinding ${evnfmnamespace}