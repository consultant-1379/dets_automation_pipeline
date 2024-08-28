#!/bin/bash


VNFLCM_IP=$1
ENV_NAME=$2
BUILD_NUMBER=$3
DATE=$4


[ -s backups_to_delete ] || { echo "No backups to delete";  exit 0; }

echo "checking connection to $VNFLCM_IP"
curl -kI  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions  | grep 200 || { echo "cannot connect to $VNFLCM_IP"; exit 1; }

flow_version=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions | jq -r  .[].definitionId | grep -i  CleanupBackups  | awk -F'.--.' '{print $2}' | uniq | sort -r | head -1)
flow_name=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/definitions | jq -r  .[].definitionId | grep -i  CleanupBackups | grep -i $flow_version)

while read backup_to_delete
do 
    response=$(curl -sk  -H "Content-Type: application/json" -d "{ \"definitionId\" : \"${flow_name}\", \"businessKey\" : \"${ENV_NAME}_cleanup_${BUILD_NUMBER}_$DATE\",  \"variables\" : { \"cleanupOptions\" : { \"type\" : \"String\", \"value\" : \"selectBackup\" } } }" -X POST https://${VNFLCM_IP}/wfs/rest/instances)
    instance_id=$(echo "$response" | jq -r .instanceId)
    business_key=$(echo "$response" | jq -r .businessKey)
    sleep 5


    usertask_id=$(curl -sk  -X GET  https://${VNFLCM_IP}/wfs/rest/usertasks?instance_id=$instance_id | jq -r  .[].usertaskId)

    backup_select_response=$(curl -sk   -H "Content-Type: application/json" -d "{ \"variables\" : { \"tagSelection\" : { \"type\" : \"String\", \"value\" : \"$backup_to_delete\"  }}}"  -X POST https://${VNFLCM_IP}/wfs/rest/usertasks/$usertask_id/complete)


    is_active=true

    while  [[ $is_active == true ]]
    do 
        sleep 180
        is_active=$(curl -sk -X GET https://${VNFLCM_IP}/wfs/rest/progresssummaries/$instance_id| jq -r  .active)
        echo "backup removal still runing - wating 180s"
    done

    #check if removal is done

    end_event=$(curl -sk -X GET https://${VNFLCM_IP}/wfs/rest/progresssummaries/$instance_id| jq -r  .endNodeType)

    if [[ $end_event == "noneEndEvent" ]]
    then 
        echo "Backup - $backup_to_delete - deletion successfull"
        exit 0
    else
        echo "Backup deletion failed"
        echo "PLEASE visit https://${VNFLCM_IP}/index.html#workflows/workflow/enmdeploymentworkflows.--.Cleanup Backups/workflowinstance/$instace_id"
        exit 1
    fi

done < backups_to_delete
