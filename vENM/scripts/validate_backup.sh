#!/bin/bash

VNFLCM_IP=$1
ENV_NAME=$2
BUILD_NUMBER=$3
DATE=$4

echo "checking connection to $VNFLCM_IP"
curl -kI  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions  | grep 200 || { echo "cannot connect to $VNFLCM_IP"; exit 1; }

flow_version=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions | jq -r  .[].definitionId | grep -i  BackupValidation  | awk -F'.--.' '{print $2}' | uniq | sort -r | head -1)
flow_name=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions | jq -r  .[].definitionId | grep -i  BackupValidation  | grep -i $flow_version)

response=$(curl -sk  -H "Content-Type: application/json" -d "{ \"definitionId\" : \"${flow_name}\", \"businessKey\" : \"${ENV_NAME}_validation_${BUILD_NUMBER}_$DATE\" }" -X POST https://${VNFLCM_IP}/wfs/rest/instances)
instance_id=$(echo "$response" | jq -r .instanceId)
business_key=$(echo "$response" | jq -r .businessKey)
sleep 5
select_bakup_instance_id=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/instances?businessKey=$business_key | jq -r  .[].instanceId | grep -v $instance_id)
backup_tags=$(curl -sk -X GET https://${VNFLCM_IP}/wfs/rest/instances/$select_bakup_instance_id/variables | jq -r  .backupTags.value[])
backup_to_validate=$(echo "$backup_tags" | grep ${ENV_NAME}_backup_${BUILD_NUMBER}_$DATE )
usertask_id=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/usertasks?instance_id=$select_bakup_instance_id | jq -r  .[].usertaskId)

backup_select_response=$(curl -sk   -H "Content-Type: application/json" -d "{ \"variables\" : { \"tagSelection\" : { \"type\" : \"String\", \"value\" : \"$backup_to_validate\"  }}}"  -X POST https://${VNFLCM_IP}/wfs/rest/usertasks/$usertask_id/complete)


is_active=true

while  [[ $is_active == true ]]
do 
    sleep 180
    is_active=$(curl -sk -X GET https://${VNFLCM_IP}/wfs/rest/progresssummaries/$instance_id| jq -r  .active)
    echo "Validation still runing - waiting 180s"
done

#check if validation is done

end_event=$(curl -sk -X GET https://${VNFLCM_IP}/wfs/rest/progresssummaries/$instance_id| jq -r  .endNodeType)

if [[ $end_event == "noneEndEvent" ]]
then 
    echo "Validation successfull"
    echo " Backup - ${ENV_NAME}_backup_${BUILD_NUMBER}_${DATE} - OK "
    echo "$backup_tags" | grep -v "$backup_to_validate" > backups_to_delete
    echo "Following backups marked for deletion"
    cat backups_to_delete
    exit 0
else
    echo "Validation failed"
     echo "PLEASE visit https://${VNFLCM_IP}/index.html#workflows/workflow/enmdeploymentworkflows.--.Backup Validation/workflowinstance/$instace_id"
    exit 1
fi
