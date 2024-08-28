#!/bin/bash
############################################Take Backup version 1.23.0-167######################################
#### Global Variables ####
evnfmfqdn=${1}
namespace=${2}
sftpIP=${3}
sftpuser=${4}

cd ~/
sudo rm -rf  evnfmbackup
mkdir evnfmbackup
cd evnfmbackup

## Collect deployment info

echo "Execute script for deployed evnfm version"
pwd
echo "$namespace"
VERSION=$(helm ls -n $namespace | sed -n 'n;p' | awk '{print $9;}')
LEN=$(echo $VERSION | awk '{print length}')
TRIMED_VERSION=$(echo $VERSION | cut -c 9-$LEN)
echo "Execiting script for version $TRIMED_VERSION"

# version trim to compare
trimVersion=$(echo ${TRIMED_VERSION} | cut -d '-' -f1)

#adding namespace depend on the version
versionSpecific="-n $namespace"

#base commands (view, create, export,verify, delete)
viewCommand="sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$TRIMED_VERSION backup view -h https://$evnfmfqdn"
createCommand="sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$TRIMED_VERSION backup create  -h https://$evnfmfqdn"
exportCommand="sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$TRIMED_VERSION backup export -h  https://$evnfmfqdn  -d 10.41.0.5:22/backups"
deleteCommand="sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$TRIMED_VERSION backup delete  -h https://$evnfmfqdn"

#check version and update base commands
echo "${trimVersion:: -2}"
if (( $(echo "${trimVersion:: -2} > 1.24" | bc -l) )); then
        echo "Version is greater than 1.24"
        viewCommand="$viewCommand $versionSpecific"
        createCommand="$createCommand $versionSpecific"
        exportCommand="$exportCommand $versionSpecific"
        deleteCommand="$deleteCommand $versionSpecific"
fi

#### Download and Upload deployment manager ####

echo "Downloading ${TRIMED_VERSION} Deployment manager from nexus"
CHECK_VERSION=$(sudo docker image ls | grep deployment-manager | awk '{print $2}')

echo "## Downloading the file from Nexus "
if wget -q --method=HEAD https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases/com/ericsson/oss/orchestration/eo/eric-eo-evnfm/${TRIMED_VERSION}/eric-eo-evnfm-${TRIMED_VERSION}.csar;
 then
  echo "## This version eo-evnfm csar package present in nexus"
  echo "## DOWNLOADING... "
  wget -q https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases/com/ericsson/oss/orchestration/eo/eric-eo-evnfm/${TRIMED_VERSION}/eric-eo-evnfm-${TRIMED_VERSION}.csar
  sleep 2
  echo "## extracting deployment-manager.tar file"
  unzip -j eric-eo-evnfm-${TRIMED_VERSION}.csar 'Scripts/common/deployment-manager.tar'
  echo "## docker load"
  docker load --input deployment-manager.tar
 else
  echo "## ERROR: The file not present please use correct EO version"
  exit 1
fi

echo "checking loaded deployment manager image"
f=$(sudo docker images | grep deployment-manager | grep $TRIMED_VERSION | wc -l)
if [ "$f" -eq "1" ]; then
        echo "deployment manager loaded"
else
        echo "Failed to load deployment manager, please investigate, exiting"
        echo "Cleanup residual files"
        rm -rf installed_ver.txt evnfm_versions.txt temp.txt
        exit 1
fi

#echo "## Getting eric-eo-evnfm-${TRIMED_VERSION}.csar"
#scp ~/eric-eo-evnfm-${TRIMED_VERSION}.csar ~/evnfmbackup
#sleep 1
#unzip -j eric-eo-evnfm-${TRIMED_VERSION}.csar 'Scripts/common/deployment-manager.tar'
#echo "## Load the deployment manager"
#docker load --input deployment-manager.tar
#############################################################

echo "Prepare to generate site value file"
sudo rm -rf kube_config
sudo mkdir kube_config
sudo scp /home/eccd/.kube/config kube_config/config
sudo chmod 777 kube_config/config
sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir -v /etc/hosts:/etc/hosts deployment-manager:$TRIMED_VERSION prepare --namespace ${namespace}

#### Modify EVNFM UI Password ####

sudo touch command.py
sudo chmod 777 command.py

########### Modify the site values file #############
echo "python3 -c 'import yaml;f=open(\"site_values_$TRIMED_VERSION.yaml\");y=yaml.safe_load(f);
y[\"eric-ctrl-bro\"][\"sftp\"][\"username\"] = \"sftp-user\"; 
y[\"eric-ctrl-bro\"][\"sftp\"][\"password\"] = \"DefaultP12345\";
print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" >command.py
./command.py >temp.yaml
sed -i -e 's/'\'true\''/true/g' temp.yaml
sed -i -e 's/'\'false\''/false/g' temp.yaml

cp -f temp.yaml site_values_$TRIMED_VERSION.yaml
rm -rf temp.yaml

#### List Backup ####
echo "List available bacckups"
echo "$viewCommand"
$viewCommand
$viewCommand 2>&1 | grep jenkins_backup >>temp.txt

#### Create Backup ####
echo "Creating backup using bro pod"
g=$(date | sed 's/ //g')
l=$(hostname)
echo "Taking backup "
backup_name=$(echo "$l:$g:jenkins_backup")
echo "$createCommand --name $backup_name"
$createCommand --name $backup_name

# sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup create --name $backup_name -h https://$evnfmfqdn -n $namespace
# echo "sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup create --name $backup_name -h https://$evnfmfqdn -n $namespace"


#### Check backup create error ####

echo "Checking Error in create command output"
error=$(cat logs/*_backup_create.log | grep ERROR | wc -l)
echo "error code $error"
if [ "$error" -gt "0" ]; then
        echo "ERROR: Create Backup Failed, deleting the created backup, please investigate"
        $deleteCommand --name $backup_name
        # sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup delete --name $backup_name -h https://$evnfmfqdn -n $namespace
        exit 1
fi

#### Export Backup ####

echo "Sleep for 60 Seconds"
sleep 60s

echo "$exportCommand --name $backup_name"
$exportCommand --name $backup_name
# sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup export -h https://$evnfmfqdn --name $backup_name -d 10.41.0.5:22/backups -n $namespace

# echo "sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup export -h  https://$evnfmfqdn --name $backup_name -d 10.41.0.5:22/backups -n $namespace"

#### Verify Backup ####

echo "Verify available bacckups, for the taken backup"
k=$($viewCommand 2>&1 | grep $backup_name | wc -l)
# k=$(sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup view -h https://$evnfmfqdn -n $namespace 2>&1 | grep $backup_name | wc -l)

if [ "$k" -eq "1" ]; then
        echo "Backup file has been created : $backup_name"
        echo 0 >JENKINS_BACKUP_STATUS
else
        echo "Backup creation failed, exiting, please investigate the deployment"
        echo 1 >JENKINS_BACKUP_STATUS
fi

#### Clean Up Backup ####

echo "Clean up the backup file from bro pod"
$deleteCommand --name $backup_name
# sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup delete --name $backup_name -h https://$evnfmfqdn -n $namespace
echo "Verify if the cleanup is done"
k=1
k=$($viewCommand 2>&1 | grep $backup_name | wc -l)
#k=`sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$e backup view -h https://$evnfmfqdn 2>&1 | grep $backup_name | wc -l`
# k=$(sudo docker run --rm -u $(id -u):$(id -g) -v $PWD:/workdir deployment-manager:$version backup view -h https://$evnfmfqdn -n $namespace 2>&1 | grep $backup_name | wc -l)
echo "k value is = $k"
if [ "$k" -eq "0" ]; then
        echo "Backup has been deleted"
else
        echo "Backup deletion failed, exiting, please investigate the deployment"
fi

#### Check Backup export Error ####

echo "Checking Error in export command output"
error=$(cat logs/*_backup_export.log | grep ERROR | wc -l)
echo "error code $error"
if [ "$error" -gt "0" ]; then
        echo "ERROR: Export Backup Failed, please investigate"
        echo $error >JENKINS_BACKUP_STATUS
        exit 1
fi

#### Cleanup ####	
echo "Cleanup residual files"
rm -rf installed_ver.txt evnfm_versions.txt temp.txt

##########################################################################################################