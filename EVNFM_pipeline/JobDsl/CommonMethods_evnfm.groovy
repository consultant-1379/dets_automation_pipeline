//Common function
// fetching data from minio
//evnfm task function

def clusterID() {
  clusterNameLength = "${environment_name}".length()
  if (clusterNameLength == 12){
    env.clusterID = sh (script : "echo ${environment_name} | cut -b 1-6", returnStdout: true ).trim()
    println("${env.clusterID}")
  } else {
    env.clusterID = sh (script : "echo ${environment_name} | cut -b 1-5", returnStdout: true ).trim()
    println("${env.clusterID}")
  }
}

def read_site_config_file_other_resource(){
    //env.kubeConfig = "${workspace}/.kube/${KUBE_CRED}"
    env.HELM_BINARY = "helm"
    env.helm = "docker run --rm -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} helm"
    env.kubectl = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl"
}

def fetchDataFromMinio(){
  Random rnd = new Random()
  sh "docker run --rm -v ${WORKSPACE}:${WORKSPACE} --name  minioMC${rnd.next(20000)} --entrypoint=/bin/sh ${minio_mc_docker_image} -c 'mc alias set minioAPI ${miniourl} minio ${minioP};mc cp minioAPI/${DeploymentPemFilePath} ${WORKSPACE};mc cp minioAPI/${DeploymentIPPath} ${WORKSPACE}'"
}

def getDirectorIP(){
    //echo ccd-${environment_name}.directorvip.yml
    env.checkIP = sh (script: "cat ccd-${environment_name}.directorvip.yml| grep -i director| cut -d ' ' -f1",returnStdout: true ).trim()
    env.result = sh (script: "if [[ $checkIP =~ ^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+\$ ]];then echo success; else echo failed; fi",returnStdout: true ).trim()
    if ( env.result == "success" ){
        env.directorIP = env.checkIP
    }
    else{
        env.directorIP = sh (script: "cat ccd-${environment_name}.directorvip.yml| grep -i director| cut -d ' ' -f2",returnStdout: true ).trim()
    }
//   env.directorIP = sh (script: "cat ccd-${environment_name}.directorvip.yml| grep -i director| cut -d ':' -f2",returnStdout: true ).trim()
  echo env.directorIP
}

def permissionChange(){
    sh "scp ccd-${environment_name}.director.pem ${environment_name}.director.pem"
    sh "chmod 600 ${environment_name}.director.pem"
}

//def runBackupScript(){
 //   sh "chmod +x evnfm_backup.sh"
   // sh "ssh -i ${environment_name}.director.pem eccd@${env.directorIP} -o LogLevel=error -o 'StrictHostKeyChecking no' 'bash -s ${sftpuser} ${sftppass} ${sftpip} ${evnfmfqdn} ${evnfmRbacUsername} ${evnfmRbacPassword} ${namespace}' < ./evnfm_backup.sh"
//}

def evnfmBackup(){
    sh "scp EVNFM_pipeline/JobDsl/backup/evnfmbackupScript.sh ${WORKSPACE}"
    sh "ssh -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' eccd@${env.directorIP} 'bash -s ${EVNFM_fqdn} ${namespace} ${sftpip} ${sftpuser}' < ./evnfmbackupScript.sh"
}

def evnfmDeployPreTask(){
    println "${EO_version}"
    println "${namespace}"
    sh "scp EVNFM_pipeline/JobDsl/install/evnfmDeployPreTask.sh ${WORKSPACE}"
    sh "ssh -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' eccd@${env.directorIP} 'bash -s ${EO_version} ${namespace}' < ./evnfmDeployPreTask.sh"
}

///////////////////////////////////////////////////////////////////CNIS env.//////////////////////////////////////////////////////////////////////////////////////

//detail fetch from minio 

def cnis_fetchDataFromMinio(){
  Random rnd = new Random()
  sh "docker run --rm -v ${WORKSPACE}:${WORKSPACE} --name  minioMC${rnd.next(10000)} --entrypoint=/bin/sh ${minio_mc_docker_image} -c 'mc alias set minioAPI ${miniourl} minio ${minioP};mc cp -r minioAPI/${cnis_deploymentCertPath} ${WORKSPACE}'"
  sh "echo 'Transferring certificates..'"
  if("${deployment_type}" == "install"){
    sh "scp -o LogLevel=error -o 'StrictHostKeyChecking no' *.key *.crt cenmbuild@214.5.198.4:${evnfmdeploydir}/certificates"
  }
  else{
    sh "scp -o LogLevel=error -o 'StrictHostKeyChecking no' *.key *.crt cenmbuild@214.5.198.4:${evnfmupgradedir}certificates"
  }
}

// client vm arrangement
def prepareclientvm(){
  sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@214.14.16.20 'bash /home/cenmbuild/AUTO/script/kubeconfigfile_trasfer.sh ${env.clusterID}'" 
}

def cnis_evnfmDeployPreTask(){
  sh "scp EVNFM_pipeline/JobDsl/install/cnis_evnfmDeployPreTask.sh ${WORKSPACE}"
  sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@214.5.198.4 'bash -s ${EO_version} ${namespace} ${evnfmdeploydir}' < ./cnis_evnfmDeployPreTask.sh"
}

def cnis_siteValueFilePreparation(){
  sh "scp EVNFM_pipeline/JobDsl/siteValueFile/cnis_site_values_${EO_version}.sh ${WORKSPACE}"
  sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@214.5.198.4 'bash -s ${EO_version} ${ICCR_INGRESS_IP} ${EVNFM_fqdn} ${IAM_HOSTNAME} ${EO_VNFM_REGISTRY_HOSTNAME} ${EO_HELM_REGISTRY_HOSTNAME} ${EO_GAS_HOSTNAME} ${DOCKER_REGISTRY_PASSWORD} ${DOCKER_REGISTRY_USERNAME} ${ICCR_INGRESS_IP} ${deployment_type} ${evnfmdeploydir} ${evnfmupgradedir}' < ./cnis_site_values_${EO_version}.sh"
}

def cnis_evnfmDeployUpgrade() {
    sh "scp EVNFM_pipeline/JobDsl/cnis_evnfmDeployUpgrade.sh ${WORKSPACE}"
    sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@214.5.198.4 'bash -s ${EO_version} ${namespace} ${deployment_type} ${evnfmdeploydir} ${evnfmupgradedir} ${HELM_TIMEOUT}' < ./cnis_evnfmDeployUpgrade.sh"
}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def evnfmUpgradePreTask(){
    sh "scp EVNFM_pipeline/JobDsl/upgrade/evnfmUpgradePreTask.sh ${WORKSPACE}"
    sh "ssh -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' eccd@${env.directorIP} 'bash -s ${EO_version} ${namespace}' < ./evnfmUpgradePreTask.sh"
}

def fetchcertFromMinioAndTransfer(){
  Random rnd = new Random()
  sh "docker run --rm -v ${WORKSPACE}:${WORKSPACE} --name  minioMC${rnd.next(10000)} --entrypoint=/bin/sh ${minio_mc_docker_image} -c 'mc alias set minioAPI ${miniourl} minio ${minioP};mc cp -r minioAPI/${DeploymentCertPath} ${WORKSPACE}'"
  sh "echo 'Transferring certificates..'"
  if("${deployment_type}" == "install"){
    sh "scp -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' *.key *.crt eccd@${env.directorIP}:/home/eccd/evnfmdeploy/certificates/"
  }
  else{
    sh "scp -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' *.key *.crt eccd@${env.directorIP}:/home/eccd/evnfmupgrade/certificates/"
  }
}

def siteValueFilePreparation(){
    sh "scp EVNFM_pipeline/JobDsl/siteValueFile/site_values_${EO_version}.sh ${WORKSPACE}"
    sh "ssh -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' eccd@${env.directorIP} 'bash -s ${EO_version} ${ICCR_INGRESS_IP} ${EVNFM_fqdn} ${IAM_HOSTNAME} ${EO_VNFM_REGISTRY_HOSTNAME} ${EO_HELM_REGISTRY_HOSTNAME} ${EO_GAS_HOSTNAME} ${DOCKER_REGISTRY_PASSWORD} ${DOCKER_REGISTRY_USERNAME} ${ICCR_INGRESS_IP} ${deployment_type}' < ./site_values_${EO_version}.sh"
}

def evnfmDeployUpgrade(){
    sh "scp EVNFM_pipeline/JobDsl/evnfmDeployUpgrade.sh ${WORKSPACE}"
    sh "ssh -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' eccd@${env.directorIP} 'bash -s ${EO_version} ${namespace} ${deployment_type} ${HELM_TIMEOUT}' < ./evnfmDeployUpgrade.sh"
}

def evnfmcleandown(){
    sh "scp EVNFM_pipeline/JobDsl/evnfmCleandown.sh ${WORKSPACE}"
    sh "ssh -i ${environment_name}.director.pem -o LogLevel=error -o 'StrictHostKeyChecking no' eccd@${env.directorIP} 'bash -s ${namespace} ${crd_namespace}' < ./evnfmCleandown.sh"
}

return this