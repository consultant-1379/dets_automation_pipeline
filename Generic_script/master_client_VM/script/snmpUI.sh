#! /bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <vPOD name>"
  exit 3
fi

clusterID=${1}
##########
masterOamIP=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${clusterID} | cut -d "," -f3)
SSH_OPTIONS="-o LogLevel=error -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10"

ssh ${SSH_OPTIONS} eccd@${masterOamIP} "kubectl get node -o wide | awk '{print \$1,\"\",\"ansible_host=\"\$6}' > inventory || true"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "sed -i 1d inventory || true"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "sed -i '1 i [all]' inventory || true"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "echo '[all:vars]' >> inventory || true"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "echo 'ansible_python_interpreter=/usr/bin/python3' >> inventory || true"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "echo 'ansible_ssh_common_args='-o StrictHostKeyChecking=no'' >> inventory || true"

# ansible thing
echo "################## current status ##################"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "ansible all -b -i inventory -m shell -a \"cat /etc/sysctl.conf | egrep -i 'net.ipv4.conf.all.rp_filter|net.ipv4.conf.ccd_int.rp_filter|net.ipv4.vs.conntrack'\""

echo "--------------------------------------------------------"
echo "----------------- Applying SNMP WA ---------------------"
echo "--------------------------------------------------------"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "ansible all -b -i inventory -m shell -a \"sudo sed -i '/net.ipv4.conf.all.rp_filter=1/d' /etc/sysctl.conf; sudo sysctl -p\""

echo "################## post status ##################"
ssh ${SSH_OPTIONS} eccd@${masterOamIP} "ansible all -b -i inventory -m shell -a \"cat /etc/sysctl.conf | egrep -i 'net.ipv4.conf.all.rp_filter|net.ipv4.conf.ccd_int.rp_filter|net.ipv4.vs.conntrack'\""
