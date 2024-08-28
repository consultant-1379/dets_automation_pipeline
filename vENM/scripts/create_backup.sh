#!/bin/bash

VNFLCM_IP=$1
ENV_NAME=$2
BUILD_NUMBER=$3
DATE=$4

echo "checking connection to $VNFLCM_IP"
curl -kI  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions  | grep 200 || { echo "cannot connect to $VNFLCM_IP"; exit 1; }

#here to choose lates available version
flow_version=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions | jq -r  .[].definitionId | grep -i  BackupDeployment| awk -F'.--.' '{print $2}' | uniq | sort -r | head -1)
flow_name=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions | jq -r  .[].definitionId | grep -i  BackupDeployment| grep -i $flow_version)
echo "$flow_name"

response=$(curl -sk  -H "Content-Type: application/json" -d "{ \"definitionId\" : \"${flow_name}\", \"businessKey\" : \"${ENV_NAME}_backup_${BUILD_NUMBER}_${DATE}\", \"variables\" : { \"tag\" : { \"type\" : \"String\", \"value\" : \"${ENV_NAME}_backup_${BUILD_NUMBER}_${DATE}\" } } }" -X POST https://${VNFLCM_IP}/wfs/rest/instances)
instance_id=$(echo "$response" | jq -r .instanceId)

is_active=true

while  [[ $is_active == true ]]
do 
    sleep 300
    is_active=$(curl -sk -X GET https://${VNFLCM_IP}/wfs/rest/progresssummaries/$instance_id| jq -r  .active)
    echo "Backup still runing - waiting 5 minutes"
done

#check if backup is done

end_event=$(curl -sk -X GET https://${VNFLCM_IP}/wfs/rest/progresssummaries/$instance_id| jq -r  .endNodeType)

if [[ $end_event == "noneEndEvent" ]]
then 
    echo "backup successfull - ${ENV_NAME}_backup_${BUILD_NUMBER}_${DATE} created "
    exit 0
else
    echo "BACKUP failed"
    echo "PLEASE visit https://${VNFLCM_IP}/index.html#workflows/workflow/enmdeploymentworkflows.--.Backup%20Deployment/workflowinstance/$instace_id"
    exit 1
fi
