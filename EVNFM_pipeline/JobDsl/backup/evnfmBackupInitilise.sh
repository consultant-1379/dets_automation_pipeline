#!/bin/bash
cd ~/
sudo rm -rf  evnfmbackup
mkdir evnfmbackup
cd evnfmbackup

########################Global variables###########################
#sftpuser=$sftp_user_name
#sftppass=$sftp_password
#sftpip=$sftp_password
sftpuser=$1
sftppass=$2
sftpip=$3
evnfmfqdn=$4
sftp_user_name=$5
sftp_password=$6
# sftp_server_vp5_ip=$7
namespace=$7

sftp_path="evnfm_backupScript/evnfmBackup.sh"
#### Collect Deployment Info ####
helm list -A >> temp.txt
kubectl get namespace -A >> temp.txt

echo "Execute script for deployed evnfm version"
pwd
echo "$namespace"
VERSION=$(helm ls -n $namespace | sed -n 'n;p' | awk '{print $9;}')
LEN=$(echo $VERSION | awk '{print length}')
TRIMED_VERSION=$(echo $VERSION | cut -c 9-$LEN)
echo "Execiting script for version $TRIMED_VERSION"
sudo curl  -k "sftp://$sftpuser:$sftppass@$sftpip:22/$sftp_path" -o backup$TRIMED_VERSION.sh
ls -la
sudo chmod 777 backup$TRIMED_VERSION.sh
./backup$TRIMED_VERSION.sh $evnfmfqdn $TRIMED_VERSION ${namespace} $sftpip $sftpuser $sftppass

JENKINS_BACKUP_STATUS=`cat JENKINS_BACKUP_STATUS`
if [ "$JENKINS_BACKUP_STATUS" -gt "1" ]
then
       echo "evnfm backup creation failed, please investigate"
       echo "Cleanup residual files"
       rm -rf installed_ver.txt evnfm_versions.txt temp.txt
       exit 1
else
       echo "evnfm backup creation succeeded"
fi

#### Cleanup ####	
echo "Cleanup residual files"
rm -rf installed_ver.txt evnfm_versions.txt temp.txt

#### End of Script ####
