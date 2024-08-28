#! /bin/bash

######################Variables#####################
Ironic_IP=${1}
ccdName=$(kubectl get nodes | awk '(NR==2)' | cut -d "-" -f3)
#ccdNumber=$(kubectl get node|awk '(NR==2)'|awk '{print $1}'| cut -d "-" -f3 | cut -d "v" -f2)
ironocCredentialsNamespace=$(kubectl get secret -A | grep ironic-credentials | awk '{print $1}')
HWComponentList=$(kubectl get node|awk '(NR==2)'|awk '{print $1}'|cut -d "-" -f2)
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"

etcd_Internal_IP1=$(kubectl get no -o json --selector='node-role.kubernetes.io/master'| jq -r '.items[].status.addresses[] | select(.type=="InternalIP") | .address'|sort|awk '(NR==1)')
etcd_Internal_IP2=$(kubectl get no -o json --selector='node-role.kubernetes.io/master'| jq -r '.items[].status.addresses[] | select(.type=="InternalIP") | .address'|sort|awk '(NR==2)')
etcd_Internal_IP3=$(kubectl get no -o json --selector='node-role.kubernetes.io/master'| jq -r '.items[].status.addresses[] | select(.type=="InternalIP") | .address'|sort|awk '(NR==3)')
################## Function ###############

HC_Type () {
echo ""
echo ""
echo "############ ${1} ############"
echo ""
echo ""
}

smallHC_Type () {
echo ""
echo "############ ${1} ############"
echo ""
}

Heading () {
    echo ""
    echo "+++++++++ ${1} +++++++++"
    echo ""
}

########################################################

HC_Type "All Node list"
kubectl get nodes
########################################################
sleep 1

HC_Type "All non Running pod list"
kubectl get pods -A -o wide | grep -v Running
########################################################
sleep 1

HC_Type "Check for worker non-Ready node"
kubectl get node --selector='!node-role.kubernetes.io/worker' | grep -q "Ready"
########################################################
sleep 1

HC_Type "Check for master non-Ready node"
kubectl get node --selector='!node-role.kubernetes.io/master' | grep -q "Ready"
########################################################
sleep 1

HC_Type "NOK  POD from kube-system namespace"
kubectl -n kube-system get pods | grep -v -e RESTARTS -e Running -e Succeeded -e Evicted -e Completed
########################################################
sleep 1

Heading "etcd Check"
########################################################

smallHC_Type "alarm list"
sudo /usr/local/bin/etcdctl --cacert=/etc/kubernetes/pki/etcd/ca.crt --cert=/etc/kubernetes/pki/etcd/peer.crt --key=/etc/kubernetes/pki/etcd/peer.key --endpoints="${etcd_Internal_IP1}:2379,${etcd_Internal_IP2}:2379,${etcd_Internal_IP3}:2379" alarm list
########################################################

smallHC_Type "member list"
sudo /usr/local/bin/etcdctl --cacert=/etc/kubernetes/pki/etcd/ca.crt --cert=/etc/kubernetes/pki/etcd/peer.crt --key=/etc/kubernetes/pki/etcd/peer.key --endpoints="${etcd_Internal_IP1}:2379,${etcd_Internal_IP2}:2379,${etcd_Internal_IP3}:2379" member list
########################################################

smallHC_Type "endpoint Health"
sudo /usr/local/bin/etcdctl --cacert=/etc/kubernetes/pki/etcd/ca.crt --cert=/etc/kubernetes/pki/etcd/peer.crt --key=/etc/kubernetes/pki/etcd/peer.key --endpoints="${etcd_Internal_IP1}:2379,${etcd_Internal_IP2}:2379,${etcd_Internal_IP3}:2379"  endpoint health
########################################################
sleep 1

HC_Type "fetch node list from provisioning VIP"
username=$(kubectl get secret -n ${ironocCredentialsNamespace} ironic-credentials -o yaml|grep -i username:| cut -d ":" -f2|cut -d " " -f2)
password=$(kubectl get secret -n ${ironocCredentialsNamespace} ironic-credentials -o yaml|grep -i password:| cut -d ":" -f2|cut -d " " -f2)
encodeusername=$(echo -n ${username} | base64 --decode)
encodepassword=$(echo -n ${password} | base64 --decode)
HC_Type "fetch node list from provisioning VIP"
curl -u "${encodeusername}:${encodepassword}" --connect-timeout 10 https://${Ironic_IP}:6385/v1/nodes | jq
########################################################
sleep 1

HC_Type "calico full mesh BGP peers are all UP"

sudo calicoctl node status
#######################################################
sleep 1

HC_Type "Identify ECFE frontend speakers"
kubectl get pods -A | grep frontend-speaker
########################################################
Heading "check BIRD configuration on each frontend-speaker"
sleep 1

HC_Type "check BIRD configuration on each frontend-speaker"
for i in $(kubectl get pods -n kube-system | grep frontend-speaker| awk '{print $1}') ; do kubectl exec $i -n kube-system -- birdcl show pr ; done
#######################################
sleep 1

smallHC_Type "bfd sessions"
for i in $(kubectl get pods -n kube-system | grep frontend-speaker| awk '{print $1}') ; do kubectl exec $i -n kube-system -- birdcl show bfd sessions; done
########################################################
sleep 1

########################################################
sleep 1

HC_Type "Kubernetes Storage ClassesHC_Type "
kubectl get storageclasses
########################################################
sleep 1

HC_Type "Kubernetes Storage"
kubectl get pvc -A | grep eric-lcm-container-registry
echo ""
sleep 1

HC_Type "Ingress output to check fqdn"

kubectl get ingress -A
echo ""
sleep 1

########################################################
#Heading "NTP check on each node"

#for i in $(kubectl get node -o wide | grep -v VERSION | awk '{print $6}')
#do
#  ssh ${SSH_OPTIONS} ${i} "timedatectl | grep 'synchronized'"
#  done >> /home/eccd/NTP_status

#HC_Type "each node NTP status"
#cat /home/eccd/NTP_status
########################################################
##tranfer of script for ceph check


#Heading "CCD post HC end"