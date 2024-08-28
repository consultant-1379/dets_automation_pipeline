def delete_build_utilities_image()
{
    images = sh (script: '''docker images|awk '{ print $1":"$2 }' ''', returnStdout: true ).trim()
    if ( images.contains("${cenm_utilities_docker_image}") ){
       sh '''  docker rmi -f "${cenm_utilities_docker_image}" '''
       echo "${cenm_utilities_docker_image} image removed from the slave"
}
}

def extract_jq(){
    echo "Extracting the jq software"
    sh "tar -xvf /var/Gerrit/BUR/software/jq-1.0.1.tar ; chmod +x ./jq"
}

def download_kube_config_file_from_dit(){
    env.kube_config_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cloud_native_enm_kube_config\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    env.KUBE_CRED =  sh (script : "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$kube_config_document_id\" | ./jq '.name' | sed 's/\"//g'", returnStdout: true).trim()

    env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
    env.CLIENT_MACHINE_IP_ADDRESS = sh (script : "./jq '.content.global.client_machine.ipaddress' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CLIENT_MACHINE_USERNAME = sh (script : "./jq '.content.global.client_machine.username' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CLIENT_MACHINE_TYPE = sh (script : "./jq '.content.global.client_machine.type' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()

    sh "rm -rf ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    sh "mkdir ${workspace}/Kube-Config-Files/"
    sh "touch ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    sh "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$kube_config_document_id\" | ./jq '.content' | sed 's/\"//g' >> ${workspace}/Kube-Config-Files/${KUBE_CRED}"
}

def read_site_config_info_from_dit(){

    env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()

    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"

    env.NAMESPACE =  sh (script : "./jq '.content.global.namespace' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.EMAIL_LIST= sh (script : "./jq '.content.global.email_id' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()

    env.SFTP_MACHINE_HOSTNAME = sh (script : "./jq '.content.global.sftp_machine.hostname' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SFTP_MACHINE_USERNAME = sh (script : "./jq '.content.global.sftp_machine.users.username' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SFTP_MACHINE_PASSWORD = sh (script : "./jq '.content.global.sftp_machine.users.password' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()

    env.CLIENT_MACHINE_IP_ADDRESS = sh (script : "./jq '.content.global.client_machine.ipaddress' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CLIENT_MACHINE_USERNAME = sh (script : "./jq '.content.global.client_machine.username' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CLIENT_MACHINE_TYPE = sh (script : "./jq '.content.global.client_machine.type' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SFTP_PATH = sh (script : "./jq '.content.global.backup.sftp_path' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SFTP_IP = sh (script : "./jq '.content.global.backup.sftp_ip' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.HELM_BINARY = "helm"
    env.kubeConfig = "${workspace}/.kube/${KUBE_CRED}"
    env.helm = "docker run --rm -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} helm"
    env.kubectl = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl"
}

def set_kube_config_file(){
    sh 'mkdir -p ${PWD}/.kube && chmod 775 ${PWD}/.kube && cp -v ${PWD}/Kube-Config-Files/${KUBE_CRED} ${PWD}/.kube/${KUBE_CRED} && chmod 620 ${PWD}/.kube/${KUBE_CRED}'
}

def brocli_configuration(){
   env.brocli="${kubectl} exec deploy/brocli -n ${NAMESPACE} -i -- brocli"
}

def backup_rollback() {
  sh "${brocli} create $backup_name --scope $backup_scope"
  sh "${brocli} show $backup_name --scope $backup_scope"
}

def backup_restore() {
   sh "${brocli} create $backup_name"
   sh "${brocli} show $backup_name"
}

def export_backup(){
  echo "export the backup to sftp server"
  sh "${brocli} export $backup_name --uri 'sftp://sftpuser@${SFTP_IP}:22${SFTP_PATH}' --password 'DefaultP12345'"
}

def fetchDataFromMinio(){
  sh "docker run --rm -v ${HOME_DIR}:${HOME_DIR} --name  minioMC --entrypoint=/bin/sh ${minio_mc_docker_image} -c 'mc alias set minioAPI ${miniourl} minio ${minioP};mc cp minioAPI/${DeploymentPemFilePath} ${HOME_DIR};mc cp minioAPI/${DeploymentIPPath} ${HOME_DIR}'"
}

def getDirectorIP(){
  env.directorIP = sh (script: "cat ccd-${environment_name}.directorvip.yml| grep -i director| cut -d ':' -f2",returnStdout: true ).trim()
  echo env.directorIP
}

def UploadBackupScript(){
  sh "scp -i ccd-${environment_name}.director.pem -o 'LogLevel=error' -o 'StrictHostKeyChecking no' evnfm_backup.sh eccd@${env.directorIP}:${CCDDIR}"
  sh "ssh -i ccd-${environment_name}.director.pem -o 'LogLevel=error' -o 'StrictHostKeyChecking no' -tt eccd@${env.directorIP} 'chmod -x evnfm_backup.sh'"
}

return this