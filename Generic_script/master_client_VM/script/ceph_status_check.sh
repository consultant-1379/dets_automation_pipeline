#! /bin/bash
###
SSH_OPTIONS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=10 -o LogLevel=error"


 rm -rf /home/eccd/NTP_status
echo "NTP Check"

for i in $(kubectl get node -o wide | grep -v VERSION | awk '{print $6}')
do
  ssh ${SSH_OPTIONS} ${i} "timedatectl | grep 'synchronized'"
  done >> /home/eccd/NTP_status

echo "each node NTP status"
cat /home/eccd/NTP_status

####
echo ""
echo "CEPH level status "
/var/lib/eccd/ceph_cli.sh status

echo "CEPH device list"

/var/lib/eccd/ceph_cli.sh device ls
########################################################

#HC_Type "mute reclaim check"
#HealthCH=$(/var/lib/eccd/ceph_cli.sh status | grep health:| cut -d ":" -f2 | xargs)

#if [[ ${HealthCH} == "HEALTH_OK" ]]
#then
#  echo 'ceph health is already set and its ok'
#  /var/lib/eccd/ceph_cli.sh status
#else
#  echo "Need manual investigation"
#  /var/lib/eccd/ceph_cli.sh status
#fi
########################################################
#sleep 1
######
