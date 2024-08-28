///////////////////////////////////////////////////////////////Common functions

//// Infra

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
    sh "tar -xvf software/jq-1.0.1.tar ; chmod +x ./jq"
}

def createDir(){
    sh "mkdir -p ${WORKSPACE}/Kube-Config-Files"
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
    env.integration_value_file_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_integration_values\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()

    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$integration_value_file_document_id'>deployment_integration_values_file.json"
    env.FM_VIP_ADDRESS = sh (script : "./jq '.content.global.vips.fm_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SVC_FM_VIP_FWD_IPADDRESS = sh (script : "./jq '.content.global.vips.svc_FM_vip_fwd_ipaddress' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CM_VIP_ADDRESS = sh (script : "./jq '.content.global.vips.cm_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.PM_VIP_ADDRESS = sh (script : "./jq '.content.global.vips.pm_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.AMOS_VIP_ADDRESS = sh (script : "./jq '.content.global.vips.amos_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.ELEMENT_MANAGER_VIP = sh (script : "./jq '.content.global.vips.element_manager_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SCRIPT_VIP_ADDRESS = sh (script : "./jq '.content.global.vips.general_scripting_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.VISINAMINGSB_SERVICE = sh (script : "./jq '.content.global.vips.visinamingsb_service' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.ITSERVICES_0_VIP_ADDRESS = sh (script : "./jq '.content.global.vips.itservices_0_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.ITSERVICES_1_VIP_ADDRESS = sh (script : "./jq '.content.global.vips.itservices_1_vip_address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SVC_FM_VIP_IPV6ADDRESS = sh (script : "./jq '.content.global.vips.svc_FM_vip_ipv6address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SVC_CM_VIP_IPV6ADDRESS = sh (script : "./jq '.content.global.vips.svc_CM_vip_ipv6address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SVC_PM_VIP_IPV6ADDRESS = sh (script : "./jq '.content.global.vips.svc_PM_vip_ipv6address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.AMOS_SERVICE_IPV6_IPS = sh (script : "./jq '.content.global.vips.amos_service_IPv6_IPs' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SCRIPTING_SERVICE_IPV6_IPS = sh (script : "./jq '.content.global.vips.scripting_service_IPv6_IPs' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.VISINAMINGSB_SERVICE_IPV6_IPS = sh (script : "./jq '.content.global.vips.visinamingsb_service_IPv6_IPs' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.ITSERVICES_SERVICE_0_IPV6_IPS = sh (script : "./jq '.content.global.vips.itservices_service_0_IPv6_IPs' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.ITSERVICES_SERVICE_1_IPV6_IPS = sh (script : "./jq '.content.global.vips.itservices_service_1_IPv6_IPs' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SVC_FM_VIP_FWD_IPV6ADDRESS = sh (script : "./jq '.content.global.vips.svc_FM_vip_fwd_ipv6address' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SECURITYSERVICELOADBALANCER_IP = sh (script : "./jq '.content.global.loadBalancerIP.securityServiceLoadBalancerIP' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.SECURITYSERVICELOADBALANCERIP_IPV6 = sh (script : "./jq '.content.global.loadBalancerIP.securityServiceLoadBalancerIP_IPv6' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.INGRESSCONTROLLERLOADBALANCERIP = sh (script : "./jq '.content.global.loadBalancerIP.ingressControllerLoadBalancerIP' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.INGRESSCONTROLLERLOADBALANCERIP_IPV6 = sh (script : "./jq '.content.global.loadBalancerIP.ingressControllerLoadBalancerIP_IPv6' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.LOADBALANCER_IP = sh (script : "./jq '.content.\"eric-oss-ingress-controller-nx\".service.loadBalancerIP' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.LOADBALANCERIP_IPV6 = sh (script : "./jq '.content.\"eric-oss-ingress-controller-nx\".service.loadBalancerIP_IPv6' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.ENM_LAUNCHER_HOSTNAME= sh (script : "./jq '.content.global.ingress.enmHost' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
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

def read_site_config_info_from_dit_short(){

    env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    env.integration_value_file_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_integration_values\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()

    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$integration_value_file_document_id'>deployment_integration_values_file.json"

    env.ENM_LAUNCHER_HOSTNAME= sh (script : "./jq '.content.global.ingress.enmHost' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    env.NAMESPACE =  sh (script : "./jq '.content.global.namespace' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.brocli_local = "kubectl --kubeconfig=./admin.conf exec deploy/brocli -n ${env.NAMESPACE} -- brocli"
    env.troubleshooting_local = "kubectl --kubeconfig=./admin.conf exec deploy/troubleshooting-utils -n ${env.NAMESPACE} --"
    env.configmap = "kubectl --kubeconfig=./admin.conf get cm -n ${env.NAMESPACE}"
    env.managingVM = "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm}"
}

def install_kube(){
    sh "ls -lrth ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    KUBECONFIG = "${workspace}/Kube-Config-Files/${KUBE_CRED}"
    sh "echo ${KUBECONFIG}"
    sh "install -m 600 $KUBECONFIG ./admin.conf"
}
///////////////////////////////////////////////////////////////////////// Generic One

// kubeconfig file

def set_kube_config_file(){
    sh 'mkdir -p ${PWD}/.kube && chmod 775 ${PWD}/.kube && cp -v ${PWD}/Kube-Config-Files/${KUBE_CRED} ${PWD}/.kube/${KUBE_CRED} && chmod 620 ${PWD}/.kube/${KUBE_CRED}'
}

// getting cluster ID

def clusterID() {
  env.clusterID = "${environment_name}"
}

// sftp server SELI and SERO IP check

def domainIPGet() {
  env.backupserverip_seli = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${environment_name}| cut -d \",\" -f3", returnStdout: true).trim()
  env.backupserverip_sero = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${environment_name}| cut -d \",\" -f4", returnStdout: true).trim()
}
// domain check

def domainCheck(){
  //env.backupserverip_seli = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} cat /home/cenmbuild/AUTO/script/bin/${env.BUR_INVENTORY}| grep ${env.clusterID}| cut -d \",\" -f3", returnStdout: true).trim()
  //env.backupserverip_sero = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} cat /home/cenmbuild/AUTO/script/bin/${env.BUR_INVENTORY}| grep ${env.clusterID}| cut -d \",\" -f4", returnStdout: true).trim()
  backup_path = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${environment_name}| cut -d \",\" -f5", returnStdout: true).trim()
  if( "${Domain}" == "SELI" ){
    env.sftp_url = "sftp://cnsftpuser@${env.backupserverip_seli}:22/${backup_path}/"
  }else{
    println "SERO server being choose here"
  }
  if( "${Domain}" == "SERO"){
    env.sftp_url = "sftp://cnsftpuser@${env.backupserverip_sero}:22/${backup_path}/"
  }else{
    println "SELI server being choose here"
  }
}
// client VM 

def countCheckfromList(){
    totalcENMcount = sh (script : "cat ${WORKSPACE}/Generic_script/master_client_VM/script/bin/cenm_list| wc -l", returnStdout: true).trim()
    echo "Total Number of cENM present in the list - ${totalcENMcount}"
    int INTtotalcENMcount = Integer.parseInt(totalcENMcount)
    return INTtotalcENMcount
}

def clientvmlist(){
  if( "${environment_name}" == "ALL"){
    totalCount = countCheckfromList()
    for(int i=1;i<=totalCount;i++){
    env.deploymentName = sh (script : "cat ${WORKSPACE}/Generic_script/master_client_VM/script/bin/cenm_list| awk '(NR==$i)'", returnStdout: true).trim()
    env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$env.deploymentName\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
    env.CLIENT_MACHINE_IP_ADDRESS = sh (script : "./jq '.content.global.client_machine.ipaddress' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    //env.TARGET_DOCKER_REGISTRY_URL = sh (script : "./jq '.content.global.registry.hostname' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    sh "echo ${env.deploymentName} ${CLIENT_MACHINE_IP_ADDRESS} >> ${WORKSPACE}/clientvmInfo"
    sh "cat ${WORKSPACE}/clientvmInfo"
    }
  }else {
    env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
    env.CLIENT_MACHINE_IP_ADDRESS = sh (script : "./jq '.content.global.client_machine.ipaddress' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.TARGET_DOCKER_REGISTRY_URL = sh (script : "./jq '.content.global.registry.hostname' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    println "Client VM for cENM - $environment_name = ${CLIENT_MACHINE_IP_ADDRESS}"
    }
}

///////////////////////////////////   passwordless login check function /////

def passwordless_login_test_from_Jenkins_to_cvm(clientMachine) {
  loginCheck = sh (script : "timeout 7s ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@$clientMachine echo > /dev/null 2>&1 && echo \"Success\" || echo \"No Connection\"", returnStdout: true).trim()
  return loginCheck
}

def docker_login_test() {
  docker_login_result = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${clientMachineIP} docker login ${env.TARGET_DOCKER_REGISTRY_URL} -u admin -p dets123 > /dev/null 2>&1 && echo \"OK\" || echo \"NOK\"", returnStdout: true).trim()
  return docker_login_result
}

def cvm_update_pre_check() {
  result = passwordless_login_test_from_Jenkins_to_cvm("${clientMachineIP}")
  echo "${result}"
  if( "${result}" != "Success" ){
    error("passwordless connection from jenkins agent to client machine ${clientMachine} is not set, update the private key")
  }
  //docker login test
  docker_login_result = docker_login_test()
  if ( "${docker_login_result}" != "OK" ){
    mcvm_IP = cvmIpValidationCheck()
    echo "${mcvm_IP}"
    sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${mcvm_IP} bash +x /home/cenmbuild/AUTO/script/kubeCertCopyFromCcd.sh ${env.clusterID}|| echo \"something nok\""
    sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${mcvm_IP} ansible-playbook -i /home/cenmbuild/AUTO/ansible/inventory /home/cenmbuild/AUTO/ansible/copyKubeCertToCvm.yaml"
    docker_login_result2 = docker_login_test()
    if ( "${docker_login_result}" != "OK" ){
      error("something is nok, please check manually")
    }
    // copy the file to cvm and run the ansible command
  } else {
    echo "Client machine are ok to use for the cENM ${environment_name}"
  }
}
//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// experimental

def master_cvm_connection_check(){
  def list_of_cvm = ['214.14.16.20', '214.14.16.36', '214.5.198.4', '214.5.198.30']
  //def list_of_cvm = ['214.14.16.204', '214.5.198.44', '214.14.16.436', '214.5.198.1948']
  for(def cvm:list_of_cvm){
    def loginCheck = sh (script : "timeout 7s ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${cvm} echo > /dev/null 2>&1 && echo \"Success\" || echo \"No Connection\"", returnStdout: true).trim()
    if( "${loginCheck}" == 'Success'){
      return cvm
    }else{
      echo "Warning: master client machine IPs ${cvm} is not accessible from Jenkinks agent server, checking other vm"
    }
  }
}

def cvmIpValidationCheck(){
  mcvm = master_cvm_connection_check()
  if("${mcvm}" == 'null'){
    echo "ERROR: No master client IP from the pool is reachable from jenkins server"
    echo "ERROR: Use manual or contact ekasviv for more troubleshooting"
    error('Exiting the Job as this is much needed requirment')
  }else{
    return mcvm
  }
}
//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////
def cvm_update() {
  echo "${detsusername}"
  echo "${detspassword}"
  newClientMachineIP="${clientMachineIP}"
  env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
  sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
  env.CLIENT_MACHINE_IP_ADDRESS = sh (script : "./jq '.content.global.client_machine.ipaddress' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
  println "${env.CLIENT_MACHINE_IP_ADDRESS}"
  println "${newClientMachineIP}"
  if( "${env.CLIENT_MACHINE_IP_ADDRESS}" != "${newClientMachineIP}" ) {
      sh "./jq '.content.global.client_machine.ipaddress = \"$clientMachineIP\"' deployment_site_config_information.json > deployment_site_config_information_new.json"
      sh "curl -u '${detsusername}:${detspassword}' -X PUT 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id' -H 'accept: application/json' -H 'Content-Type: application/json' -d @deployment_site_config_information_new.json"
      sh "echo 'checking'"
      //CommonMethods_cenm.clientvmUpdate()
  }else {
      println "Client IP requested is same as DIT update"
  }
}

def clientvmUpdate(){
  sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} 'bash +x /home/cenmbuild/AUTO/script/certUpdateonclientVM.sh ${env.clusterID} ${clientMachineIP}'"
}
// troubleshooting utils

def troubleshooting_utils_configuration(){
    env.utils="${kubectl} exec deploy/troubleshooting-utils -n ${NAMESPACE} -i --"
}

////////////////////////////////////////////////////////////////// pre CCD deployment task running on CNIS

// mcvm ssh passwordless connection check

def mcvmMasterSshCheck(){
  env.ccdmasteroamip = sh (script : "cat ${WORKSPACE}/inventory.csv | grep ${env.clusterID} | cut -d \",\" -f3", returnStdout: true).trim()
  println("############# Master Client VM to CCD Master node connection check #############")
  env.loginStatus = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} 'ssh -oBatchMode=yes -o LogLevel=error -o \"StrictHostKeyChecking no\" eccd@${env.ccdmasteroamip} echo > /dev/null 2>&1 && echo \"Success\" || echo \"No Connection\"'", returnStdout: true).trim()
  if(env.loginStatus == 'No Connection'){
    println("login from ${managing_vm} master client to CCD master server with oam IP ${env.ccdmasteroamip} not present")
    println("## ---- stopping the job")
    sh "exit 1"
  }
  else{
    println("Passwordless connection is ok")
  }
}

// ccd level version check

def ccdLvlVersionCheck(){
  println("############# CCD Level Version check #############")
  sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} 'bash +x /home/cenmbuild/AUTO/script/ccdVersionCheck.sh ${env.clusterID}'"
  println("## Kubernetes client version ")
  sh "${env.kubectl} version --short --client"
  println("## cluster info")
  sh "${env.kubectl} cluster-info"
  println("## total nodes")
  sh "${env.kubectl} get nodes"
  println("## Helm version check")
  sh "${env.kubectl} version --client --short"
}

// cENM ericingress router label

def labelRouterNodes(){
    n1nodeName = sh (script : "${env.kubectl} get nodes | grep worker | awk \'{print \$1}\'| sort | tail -2 | awk NR==1", returnStdout: true ).trim()
    n2nodeName = sh (script : "${env.kubectl} get nodes | grep worker | awk \'{print \$1}\'| sort | tail -2 | awk NR==2", returnStdout: true ).trim()
    n1nodeLabelCheck = sh (script : "${env.kubectl} describe node ${n1nodeName} | grep \'node=ericingress\'||echo ''", returnStdout: true ).trim()
    n2nodeLabelCheck = sh (script : "${env.kubectl} describe node ${n2nodeName} | grep \'node=ericingress\'||echo ''", returnStdout: true ).trim()
    if ("${n1nodeLabelCheck}" == "") {
      println("labeling ${n1nodeName} node for routing")
      sh "${env.kubectl} label nodes ${n1nodeName} node=ericingress"
    } else {
      println ("Label of router on ${n1nodeName} node is already present")
    }
    if ("${n2nodeLabelCheck}" == "") {
      println("labeling ${n2nodeName} node for routing")
      sh "${env.kubectl} label nodes ${n2nodeName} node=ericingress"
    } else {
      println ("Label of router on ${n2nodeName} node is already present")
    }
}

// cENM alive check

def smokeTest() {
   if(env.CLIENT_MACHINE_TYPE =='client_machine' && env.deployment_mechanism =='csar'){
      response = sh (script: "ssh -o 'LogLevel=error' -o 'StrictHostKeyChecking no'  ${CLIENT_MACHINE_USERNAME}@${CLIENT_MACHINE_IP_ADDRESS} 'curl -4 --insecure -X POST -d \"IDToken1=Administrator&IDToken2=TestPassw0rd\" https://${ENM_LAUNCHER_HOSTNAME}/login -H \"Content-Type: application/x-www-form-urlencoded\" -H \"Accept-Encoding: gzip,deflate\" -H \"Accept: */*\" -L -H \"cache-control: no-cache\"'", returnStdout: true ).trim()
    }
   else{
      response = sh (script: "curl -4 --insecure -X POST -d \"IDToken1=Administrator&IDToken2=TestPassw0rd\" https://${ENM_LAUNCHER_HOSTNAME}/login -H \"Content-Type: application/x-www-form-urlencoded\" -H \"Accept-Encoding: gzip,deflate\" -H \"Accept: */*\" -L -H \'cache-control: no-cache\'|| echo \'\'", returnStdout: true ).trim()
   }
    echo response
    if ( response.contains("Authentication Successful") ){
       echo "#### Success! Can login to ENM"
    } else {
       echo "_____Failed! Can\'t login to ENM____"
       sh "exit 1"
    }
}

def mcvmMasterKubeCheck() {
    response = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} 'timeout 12s kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${env.clusterID} get ns  > /dev/null 2>&1 && echo \"Success\" || echo \"NOK\"'",returnStdout: true ).trim()
    println "${response}"
    if( "${response}" == 'Success' ){
       echo "#### Success! Can connect ccd kubeapi server from mcvm"
    } else {
       echo "__Failed! Can\'t connect kubeapi server__"
       echo "Running kubeconfig file copy command"
       sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} 'bash +x /home/cenmbuild/AUTO/script/kubeconfigCopy.sh ${env.clusterID}'"
       responseCheckagain = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} 'timeout 12s kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${env.clusterID} get ns  > /dev/null 2>&1 && echo \"Success\" || echo \"NOK\"'",returnStdout: true ).trim()
       if( "${responseCheckagain}" == 'NOK' ){
        echo "____Something is nok need manual intervension____"
        sh "exit 1"
      }
    }
}

def backupHealthCheck() {
    healthOutput = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} status | grep Health | awk '{print \$2}'", returnStdout: true).trim()
    echo "${healthOutput}"
    if ( "${healthOutput}" == "Healthy") {
      echo "#### brocli is in Healthy state"
    } else {
      echo "____brocli state is unhealthy!! NOK"
      sh "exit 1"
    }
}
/*
def backupConfigurePreTask() {
    env.backupTime = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY} | grep ${environment_name}| cut -d \",\" -f2", returnStdout: true).trim()
    env.scheduleCheck = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} schedule | grep \"Export URI\" | awk '{print \$3}'", returnStdout: true).trim()
    env.scheduleInterval = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} schedule | grep \"Schedule intervals for scope DEFAULT\"|| echo ''", returnStdout: true).trim()
    env.retensionCheckDefault = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} retention show --scope DEFAULT| grep \"Backup Limit\" | awk '{print \$3}'", returnStdout: true).trim()
    env.exportPrefix = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} schedule | grep \"Backup Prefix\" | awk '{print \$3}'", returnStdout: true).trim()
    echo "${env.backupTime}"
    echo "${env.scheduleCheck}"
    echo "${env.retensionCheck}"
    echo "${env.backupserverip_seli}"
    echo "${env.backupserverip_sero}"
    echo "${env.sftp_url}"
    echo "${env.retensionCheckDefault}"
    echo "${env.exportPrefix}"
}

def retenionCheck() {
    int INTretensionCheckDefault = Integer.parseInt(env.retensionCheckDefault)
    if (INTretensionCheckDefault != 2) {
      println("#### Retension period needs to set as per STS norms")
      sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} retention set --limit 2 --purge --scope DEFAULT"
    } else {
      println("#### Retension period is upto STC norms")
    }
}

def backupScheduling() {
    cenm_version = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.configmap} eric-enm-version-configmap -oyaml | grep product-set-version | cut -d ':' -f2", returnStdout: true)trim()
    echo "${cenm_version}"
    sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} schedule set --${schedulingTask} --prefix ${env.clusterID}cenm_${cenm_version} --export --export-uri ${env.sftp_url} --export-password DefaultP12345"
}

def addscheduleInterval() {
    dateStamp = sh (script : "date +'%F' -d '1 days'", returnStdout: true).trim()
    if ("${env.scheduleInterval}" == ""){
      echo "#### Need to set one interval which will take backup in weekly basis"
      sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} schedule add --every 1w --start ${dateStamp}T${env.backupTime}:00"
      echo "#################### ${environment_name} #########################"
      sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} schedule"
    } else {
      println "#### One Interval Already set, For more new Interval, run manually or remove existing one"
    }
  }*/

// CCD - level HC

def CCD_HC(){
  println "###################################### ${environment_name} CCD Level HealthCheck ####################################"
  sh "bash +x Generic_script/master_client_VM/script/ccd_level_remote_script.sh ${env.clusterID} ${ccd_client_master_key}"
}


// cENM - Healthcheck

def cenmHCD(){
    println "## "
    println "####################################### ---- ${environment_name} cENM Healthchecks - Verbose ----  #######################################"
    sh "${env.utils} enm_hc -v"
}

def cenmHCS(){
    println "## cENM - Short"
    println "####################################### ---- ${environment_name} cENM Healthchecks ----  #######################################"
    sh "${env.utils} enm_hc"
    sh "${env.utils} enm_hc > ${environment_name}_enm_hc_report.txt"
}

def backupList_cENM(){
    sftpSeliIP = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${environment_name}| cut -d \",\" -f3", returnStdout: true).trim()
    sftpSeroIP = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${environment_name}| cut -d \",\" -f4", returnStdout: true).trim()
    sftpserverpath = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${environment_name}| cut -d \",\" -f5", returnStdout: true).trim()
    println "## cENM - Backup"
    println "####################################### ---- ${environment_name} DEFAULT ----  #######################################"
    sh "${env.brocli} list"
    println ""
    println "## cENM - Backup"
    println "####################################### ---- ${environment_name} ROLLBACK ----  #######################################"
    sh "${env.brocli} list --scope ROLLBACK"
    println "## cENM - Backup from SFTP server"
    println "####################################### ---- ${environment_name} Backup list from SELI sftp server ----  #######################################"
    sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeliIP} 'sudo ls -lrth -R /var/container_data/sftp/home/cnsftpuser/${sftpserverpath}/'"
    //sh "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' -o ConnectTimeout=3 cenmbuild@${env.backupserverip_seli} sudo ls -lrth -R /cn_backup_data/${sftpserverpath}/ || echo \"WARNING: SFTP server ${env.backupserverip_seli} is not rechable from Jenkins server \""
    echo "-----------------------------------------------------------------------------------------------------------"
    println "####################################### ---- ${environment_name} Backup list from SERO sftp server ----  #######################################"
    sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeroIP} 'sudo find /var/container_data/sftp/home/cnsftpuser/${sftpserverpath}/'"
}

def cenmVersion(){
    println "## cENM - Version"
    println "####################################### ---- ${environment_name} version ----  #######################################"
    sh "${env.utils} enm_version "
    println ""
    println "## cENM - History"
    println "####################################### ---- ${environment_name} history ----  #######################################"
    sh "${env.utils} enm_history "
}

def cenmValueFromSED(){
    sh """
        
        echo '############## ${environment_name} ##############' >> ${environment_name}_SiteValue.txt
        echo 'ENM_URL = https://${env.ENM_LAUNCHER_HOSTNAME}' >> ${environment_name}_SiteValue.txt
        echo 'FM_VIP_ADDRESS = ${env.FM_VIP_ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'SVC_FM_VIP_FWD_IPADDRESS = ${env.SVC_FM_VIP_FWD_IPADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'CM_VIP_ADDRESS = ${env.CM_VIP_ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'PM_VIP_ADDRESS = ${env.PM_VIP_ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'AMOS_VIP_ADDRESS = ${env.AMOS_VIP_ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'ELEMENT_MANAGER_VIP = ${env.ELEMENT_MANAGER_VIP}' >> ${environment_name}_SiteValue.txt
        echo 'SCRIPT_VIP_ADDRESS = ${env.SCRIPT_VIP_ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'VISINAMINGSB_SERVICE = ${env.VISINAMINGSB_SERVICE}' >> ${environment_name}_SiteValue.txt
        echo 'ITSERVICES_0_VIP_ADDRESS = ${env.ITSERVICES_0_VIP_ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'ITSERVICES_1_VIP_ADDRESS = ${env.ITSERVICES_1_VIP_ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'SVC_FM_VIP_IPV6ADDRESS = ${env.SVC_FM_VIP_IPV6ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'SVC_CM_VIP_IPV6ADDRESS = ${env.SVC_CM_VIP_IPV6ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'SVC_PM_VIP_IPV6ADDRESS = ${env.SVC_PM_VIP_IPV6ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'AMOS_SERVICE_IPV6_IPS = ${env.AMOS_SERVICE_IPV6_IPS}' >> ${environment_name}_SiteValue.txt
        echo 'SCRIPTING_SERVICE_IPV6_IPS = ${env.SCRIPTING_SERVICE_IPV6_IPS}' >> ${environment_name}_SiteValue.txt
        echo 'VISINAMINGSB_SERVICE_IPV6_IPS = ${env.VISINAMINGSB_SERVICE_IPV6_IPS}' >> ${environment_name}_SiteValue.txt
        echo 'ITSERVICES_SERVICE_0_IPV6_IPS = ${env.ITSERVICES_SERVICE_0_IPV6_IPS}' >> ${environment_name}_SiteValue.txt
        echo 'ITSERVICES_SERVICE_1_IPV6_IPS = ${env.ITSERVICES_SERVICE_1_IPV6_IPS}' >> ${environment_name}_SiteValue.txt
        echo 'SVC_FM_VIP_FWD_IPV6ADDRESS = ${env.SVC_FM_VIP_FWD_IPV6ADDRESS}' >> ${environment_name}_SiteValue.txt
        echo 'SECURITYSERVICELOADBALANCER_IP = ${env.SECURITYSERVICELOADBALANCER_IP}' >> ${environment_name}_SiteValue.txt
        echo 'SECURITYSERVICELOADBALANCERIP_IPV6 = ${env.SECURITYSERVICELOADBALANCERIP_IPV6}' >> ${environment_name}_SiteValue.txt
        echo 'INGRESSCONTROLLERLOADBALANCERIP = ${env.INGRESSCONTROLLERLOADBALANCERIP}' >> ${environment_name}_SiteValue.txt
        echo 'INGRESSCONTROLLERLOADBALANCERIP_IPV6 = ${env.INGRESSCONTROLLERLOADBALANCERIP_IPV6}' >> ${environment_name}_SiteValue.txt
        echo 'IPv4_LOADBALANCER_IP = ${env.LOADBALANCER_IP}' >> ${environment_name}_SiteValue.txt
        echo 'LOADBALANCERIP_IPV6 = ${env.LOADBALANCERIP_IPV6}' >> ${environment_name}_SiteValue.txt
        echo '############## ${environment_name} - version ##############' >> ${environment_name}_SiteValue.txt
    """
    sh "${env.utils} enm_version >> ${environment_name}_SiteValue.txt"
}


/////// Backup Check thing

// cENM alive check

def smokeTest_backup() {
  if( "${environment_name}" == "ALL"){
    sh "rm -rf ${WORKSPACE}/RunningcENM"
    totalcENMcount = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| awk '{print \$1}' | wc -l", returnStdout: true).trim()
    echo "Total Number of cENM present in the list - ${totalcENMcount}"
    int INTtotalcENMcount = Integer.parseInt(totalcENMcount)
    INTtotalcENMcount++
    for(int i=2;i<INTtotalcENMcount;i++){
      deploymentName = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}|awk '{print \$1}' | cut -d \",\" -f1 | awk '(NR==$i)'", returnStdout: true).trim()
      integration_value_file_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$deploymentName\" |./jq '.[].documents[] | select(.schema_name==\"cENM_integration_values\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
      sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$integration_value_file_document_id'>${WORKSPACE}/deployment_integration_values_file.json"
      ENM_LAUNCHER_HOSTNAME= sh (script : "./jq '.content.global.ingress.enmHost' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
      response = sh (script: "curl -4 --insecure -X POST -d \"IDToken1=Administrator&IDToken2=TestPassw0rd\" https://${ENM_LAUNCHER_HOSTNAME}/login -H \"Content-Type: application/x-www-form-urlencoded\" -H \"Accept-Encoding: gzip,deflate\" -H \"Accept: */*\" -L -H \'cache-control: no-cache\'|| echo \'\'", returnStdout: true ).trim()
      echo response
      if ( response.contains("Authentication Successful") ){
        sh "echo ${deploymentName} >> ${WORKSPACE}/RunningcENM"
      } else {
        echo "_____Failed! Can\'t login to ${deploymentName} ENM____"
        sh "echo ${deploymentName} >> ${WORKSPACE}/NotRunningcENM"
       }
    }
  } else {
    read_site_config_info_from_dit_short()
    smokeTest()
  }
}

// new test and use case will remove above if this works fine

def download_kube_config_file_from_dit_all(environment_name){

    env.kube_config_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cloud_native_enm_kube_config\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    env.KUBE_CRED =  sh (script : "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$kube_config_document_id\" | ./jq '.name' | sed 's/\"//g'", returnStdout: true).trim()
    sh "rm -rf ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    sh "mkdir -p ${workspace}/Kube-Config-Files/"
    sh "touch ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    sh "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$kube_config_document_id\" | ./jq '.content' | sed 's/\"//g' >> ${workspace}/Kube-Config-Files/${KUBE_CRED}"
}

def clusterID_Local(local_environment_name) {
  env.clusterID_Local = "${local_environment_name}"
  println("${local_environment_name}")
}

def read_site_config_info_from_dit_short_local(local_environment_name){
    sh "rm -rf ${WORKSPACE}/deployment_site_config_information.json"
    sh "rm -rf ${WORKSPACE}/deployment_integration_values_file.json"
    env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$local_environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'> ${WORKSPACE}/deployment_site_config_information.json"
    env.NAMESPACE =  sh (script : "./jq '.content.global.namespace' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.brocli_local = "kubectl --kubeconfig=${WORKSPACE}/Kube-Config-Files/${env.KUBE_CRED} exec deploy/brocli -n ${env.NAMESPACE} -- brocli"
    env.troubleshooting_local = "kubectl --kubeconfig=${WORKSPACE}/Kube-Config-Files/${env.KUBE_CRED} exec deploy/troubleshooting-utils -n ${env.NAMESPACE} --"
    sh "echo 'end of read site part'"
}

def mcvmMasterKubeCheck_BHC(kubeconfigname) {
    response = sh (script : "kubectl --kubeconfig=${WORKSPACE}/Kube-Config-Files/$kubeconfigname get ns  > /dev/null 2>&1 && echo \"Success\" || echo \"NOK\"",returnStdout: true ).trim()
    println "${response}"
    if( "${response}" == 'Success' ){
       echo "#### Success! Can connect ccd kubeapi server from Jenkins Slave"
    } else {
       echo "__FAILED : Can\'t connect kubeapi server__"
       echo "____Something is nok check DIT kubeconfig file manually _____"
    }
}

def cenmLevelBackupCheck() {
  sh "rm -rf ${WORKSPACE}/cenmBackupMatchFile.txt || true"
  dateStamp = sh (script : "date +'%F -- %H:%M'", returnStdout: true).trim()
  if( "${environment_name}" == "ALL") {
    sh "echo '_________________________________${dateStamp}___________________________________________' >> ${WORKSPACE}/cenmBackupCheckResult.txt"
    sh "echo '' >> ${WORKSPACE}/cenmBackupCheckResult.txt"
    totalcENMcount = sh (script : "cat ${WORKSPACE}/RunningcENM| wc -l", returnStdout: true).trim()
    echo "cENM Active count - ${totalcENMcount}"
    int INTtotalcENMcount = Integer.parseInt(totalcENMcount)
    INTtotalcENMcount++
    for(int i=1;i<INTtotalcENMcount;i++){
      local_environment_name = sh (script : "cat ${WORKSPACE}/RunningcENM| awk '(NR==$i)'", returnStdout: true).trim()
      clusterID_Local = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY} | cut -d \",\" -f5 | awk '(NR==$i)'", returnStdout: true).trim()
      download_kube_config_file_from_dit_all(local_environment_name)
      read_site_config_info_from_dit_short_local(local_environment_name)
      sh "echo 'check after kubeconfig variable name'"
      mcvmMasterKubeCheck_BHC(env.KUBE_CRED)
      sftpSeliIP = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${local_environment_name}| cut -d \",\" -f3", returnStdout: true).trim()
      sftpSeroIP = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${local_environment_name}| cut -d \",\" -f4", returnStdout: true).trim()
      scheduleCheck = sh (script : "${env.brocli_local} schedule | grep \"Export URI\" | awk '{print \$3}'", returnStdout: true).trim()
      //scheduleInterval = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} schedule | grep \"Schedule intervals for scope DEFAULT\"|| echo ''", returnStdout: true).trim()
      //retensionCheckDefault = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm} ${env.brocli_local} retention show --scope DEFAULT| grep \"Backup Limit\" | awk '{print \$3}'", returnStdout: true).trim()
      lastCreatedBackup = sh (script : "${env.brocli_local} schedule | grep \"Last Created Backup\" | awk '{print \$4}'", returnStdout: true).trim()
      println "#### brocli configuration for ${local_environment_name}"
      sh "${env.brocli_local} schedule"
      echo "################ ---------------------------------------------------------- #################"
      echo "${scheduleCheck}"
      //echo "${scheduleInterval}"
      //echo "${retensionCheckDefault}"
      sh "echo '///////////////////// ## ${local_environment_name} ## /////////////////////' >> ${WORKSPACE}/cenmBackupCheckResult.txt"
      sh "${env.brocli_local} schedule >> ${WORKSPACE}/cenmBackupCheckResult.txt"
      sh "echo '-----------------------------++++++++-------------------------------' >> ${WORKSPACE}/cenmBackupCheckResult.txt"
      sh "echo '' >> ${WORKSPACE}/cenmBackupCheckResult.txt"
      cenmBackupMatch(local_environment_name,sftpSeliIP,sftpSeroIP,lastCreatedBackup)
    }
  } else {
    clusterID()
  }
}

def sftpServerLevelCheck() {
  dateStamp = sh (script : "date +'%F -- %H:%M'", returnStdout: true).trim()
  if( "${environment_name}" == "ALL") {
    sh "echo '_________________________________${dateStamp}___________________________________________' >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt"
    sh "echo '' >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt"
    totalcENMcount = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY} | cut -d \",\" -f1 | wc -l", returnStdout: true).trim()
    echo "Total cENM env count - ${totalcENMcount}"
    int INTtotalcENMcount = Integer.parseInt(totalcENMcount)
    INTtotalcENMcount++
    for(int i=2;i<INTtotalcENMcount;i++){
      local_environment_name = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}|cut -d \",\" -f1| awk '(NR==$i)'", returnStdout: true).trim()
      clusterID_Local = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY} | cut -d \",\" -f5 | awk '(NR==$i)'", returnStdout: true).trim()
      sftpSeliIP = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${local_environment_name}| cut -d \",\" -f3", returnStdout: true).trim()
      sftpSeroIP = sh (script : "cat ${WORKSPACE}/${env.BUR_INVENTORY}| grep ${local_environment_name}| cut -d \",\" -f4", returnStdout: true).trim()
      echo "${clusterID_Local}"
      echo "${sftpSeliIP}"
      echo "${sftpSeroIP}"
      seliValueFile = sh (script : "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeliIP} 'sudo ls -lrth -R /var/container_data/sftp/home/cnsftpuser/${clusterID_Local}/' || echo \"FILE NOT PRESENT\"", returnStdout: true).trim()
      seroValueFile = sh (script : "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeroIP} 'sudo ls -lrth -R /var/container_data/sftp/home/cnsftpuser/${clusterID_Local}/' || echo \"FILE NOT PRESENT\"", returnStdout: true).trim()
      echo "${seliValueFile}"
      echo "${seroValueFile}"
      sh "echo '///////////////////// ## ${local_environment_name} - sftp server SELI ## /////////////////////' >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt"
      sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeliIP} sudo ls -lrth -R /var/container_data/sftp/home/cnsftpuser/${clusterID_Local}/ >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt || echo \"FILE NOT PRESENT\""
      sh "echo '--------------------------------------------++++++++--------------------------------------------' >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt"
      sh "echo '///////////////////// ## ${local_environment_name} - sftp server SERO ## /////////////////////' >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt"
      sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeroIP} sudo ls -lrth -R /var/container_data/sftp/home/cnsftpuser/${clusterID_Local}/ >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt || echo \"FILE NOT PRESENT\" "
      sh "echo '--------------------------------------------++++++++--------------------------------------------' >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt"
      sh "echo '' >> ${WORKSPACE}/sftp_cenmBackupCheckResult.txt"
    }
  } else {
    clusterID()
  }
}

def cronjobCheck() {
  def sftpServerIpList = ["10.82.14.6","10.41.3.6"]
  sftpServerIpList.each { serverip ->
    sh "echo '///////////////////// ## ++ ${serverip} ++ ## /////////////////////' >> ${WORKSPACE}/sftpServerChecks"
    sh "echo '______________________________________ #### -- CRONJOBS' >> ${WORKSPACE}/sftpServerChecks"
    sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${serverip} sudo crontab -l >> ${WORKSPACE}/sftpServerChecks"
    sh "echo '______________________________________ #### -- /cn_backup_data Filesystem space' >> ${WORKSPACE}/sftpServerChecks"
    sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${serverip} df -h /var/container_data/sftp/ >> ${WORKSPACE}/sftpServerChecks"
    sh "echo '______________________________________ #### -- Retention script' >> ${WORKSPACE}/sftpServerChecks"
    sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${serverip} sudo ls -lrth /var/container_data/sftp/conf/sftp_lcm/ >> ${WORKSPACE}/sftpServerChecks"
    sh "echo '______________________________________ #### -- root access allow config' >> ${WORKSPACE}/sftpServerChecks"
    sh "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${serverip} sudo cat /etc/ssh/sshd_config | grep PermitRootLogin | grep -v 'without-password' >> ${WORKSPACE}/sftpServerChecks"
    sh "echo '--------------------------------++++++++--------------------------------' >> ${WORKSPACE}/sftpServerChecks"
    sh "echo '' ${WORKSPACE}/sftpServerChecks"
  }
}

def cenmBackupMatch(local_environment_name,sftpSeliIP,sftpSeroIP,lastCreatedBackup) {
  seliFileSearchInsftpServer = sh (script : "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeliIP} sudo find /var/container_data/sftp/home/cnsftpuser/ -type f | grep \"${lastCreatedBackup}\" || echo ''", returnStdout: true).trim()
  seroFileSearchInsftpServer = sh (script : "ssh -p 1022 -i ${sftp_backup_seli} -o LogLevel=error -o 'StrictHostKeyChecking no' ubuntu@${sftpSeroIP} sudo find /var/container_data/sftp/home/cnsftpuser/ -type f | grep \"${lastCreatedBackup}\" || echo ''", returnStdout: true).trim()
  echo "${seliFileSearchInsftpServer}"
  echo "${seroFileSearchInsftpServer}"
  if ( "${seliFileSearchInsftpServer}" != '' ) {
    echo "## Last taken Backup file found in SELI server"
    sh "echo ${local_environment_name} -- SELI -- ${sftpSeliIP} -- OK >> ${WORKSPACE}/cenmBackupMatchFile.txt"
  } else {
    echo "## Last Backup taken file not found"
    sh "echo ${local_environment_name} -- SELI -- ${sftpSeliIP} -- NOK >> ${WORKSPACE}/cenmBackupMatchFile.txt"
  }
  if ( "${seroFileSearchInsftpServer}" != '' ) {
    echo "## Last taken Backup file found in SERO server"
    sh "echo ${local_environment_name} -- SERO -- ${sftpSeroIP} -- OK >> ${WORKSPACE}/cenmBackupMatchFile.txt"
  } else {
    echo "## Last Backup taken file not found"
    sh "echo ${local_environment_name} -- SERO -- ${sftpSeroIP} -- NOK >> ${WORKSPACE}/cenmBackupMatchFile.txt"
  }
  sh "echo '-----------++++-----------'"
}

def lastBackupCheck() {
  println "######################################################################################################################"
  println "----------------------------------------------------------------------------------------------------------------------"
  lastbackupCheckFile = sh (script : "cat ${WORKSPACE}/cenmBackupMatchFile.txt | grep NOK || echo ''", returnStdout: true).trim()
  if ( "${lastbackupCheckFile}" == '' ) {
    println "#### ALL cENM last backup exported to sftp server and RSYNC happened correctly"
  } else {
    println "##Some cENM last backup data not present on the sftp server"
    println "##List are --"
    echo "${lastbackupCheckFile}"
  }
  println "----------------------------------------------------------------------------------------------------------------------"
  println "######################################################################################################################"  
}
///////// backup and restore

def brocli_configuration(){
    env.brocli="${kubectl} exec deploy/brocli -n ${NAMESPACE} -i -- brocli"
}

def backup_rollback() {
    Random rnd = new Random()
    env.generatednm = "${rnd.next(200)}"
    env.R_backupservername = "${environment_name}_${env.generatednm}"
    sh "${brocli} create ${R_backupservername} --scope ${backup_scope}"
    sh "${brocli} show ${R_backupservername} --scope ${backup_scope}"
}

def backup_restore() {
    Random rnd = new Random()
    env.generatednm = "${rnd.next(200)}"
    env.D_backupservername = "${environment_name}_${env.generatednm}"
    sh "${brocli} create ${env.D_backupservername}"
    sh "${brocli} show ${env.D_backupservername}"
}

def export_backup(){
  //env.backupserverip = sh (script : "cat ${WORKSPACE}/Generic_script/master_client_VM/script/bin/inventory.csv | grep ${env.clusterID} | cut -d ',' -f6", returnStdout: true).trim()
  if( "${sftp_username}" == "sftpuser" ){
    echo "Exporting backup to sftpuser server"
    sh "${brocli} export ${env.D_backupservername} --uri 'sftp://sftpuser@10.82.13.60:22/CNIS/${env.clusterID}/' --password 'DefaultP12345'"
  }else {
    println "checking other sftp user"
  }
  if( "${sftp_username}" == "cnsftpuser" ){
    echo "Exporting backup to cnsftpuser server"
    sh "${brocli} export ${env.D_backupservername} --uri '${env.sftp_url}' --password 'DefaultP12345'"    
  }else{
    print "something went wrong"
  }
}

def import_backup(){
  //env.backupserverip = sh (script : "cat ${WORKSPACE}/Generic_script/master_client_VM/script/bin/inventory.csv | grep ${env.clusterID} | cut -d \",\" -f6", returnStdout: true).trim()
  if( "${sftp_username}" == "sftpuser" ){
    echo "Importing backup from sftpuser server"
    sh "${brocli} import ${backup_name} --uri 'sftp://sftpuser@10.82.13.60:22/CNIS/${env.clusterID}/' --password 'DefaultP12345'"
  }else {
    println "checking other user"
  }
  if( "${sftp_username}" == "cnsftpuser" ){
    echo "Importing backup from cnsftpuser server"
    sh "${brocli} import ${backup_name} --uri '${env.sftp_url}' --password 'DefaultP12345'"    
  }else{
    print "something is wrong"

  }
}

def create_sftpmachine_secret(){
    env.sftp_secret_name_old = "external-storage-credentials"
    if(env.CLIENT_MACHINE_TYPE =='client_machine' && env.deployment_mechanism =='csar'){
        sh'''
           ssh -o 'LogLevel=error' -o 'StrictHostKeyChecking no' ${CLIENT_MACHINE_USERNAME}@${CLIENT_MACHINE_IP_ADDRESS} "${cenmbuildutilities_client}" kubectl delete secret "${sftp_secret_name_old}" -n ${NAMESPACE} || true
        '''
        sh'''
           ssh -o 'LogLevel=error' -o 'StrictHostKeyChecking no' ${CLIENT_MACHINE_USERNAME}@${CLIENT_MACHINE_IP_ADDRESS} "${cenmbuildutilities_client}" kubectl create secret generic "${sftp_secret_name_old}" --from-literal=externalStorageURI="${sftp_url}" --from-literal=externalStorageCredentials="DefaultP12345" -n ${NAMESPACE}
        '''
    }
    else{
        sh '''
           ${kubectl} delete secret "${sftp_secret_name_old}" -n ${NAMESPACE} || true
        '''
        sh'''
           ${kubectl} create secret generic "${sftp_secret_name_old}" --from-literal=externalStorageURI="${sftp_url}" --from-literal=externalStorageCredentials="DefaultP12345" -n ${NAMESPACE}
        '''
    }
}

///////////////////////////////////////  download check

def extract_jq_local(){
    echo "Extracting the jq software"
    sh "tar -xvf /tmp/test_ekasviv/jq-1.0.1.tar ; chmod +x ./jq"
}

def version_download_check() {
    //env.troubleshooting_local = "kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${env.clusterID} exec deploy/troubleshooting-utils -n ${env.NAMESPACE} --"
    //env.configmap = "kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${env.clusterID} get cm -n ${env.NAMESPACE}"
  deploymentType = sh (script : "${troubleshooting_local} enm_version | grep 'ENM SPRINT_TAG'|| echo ''", returnStdout: true).trim()
  env.productSetVersion = sh (script : "${managingVM} ${configmap} eric-enm-version-configmap -oyaml | grep 'product-set-version' | cut -d ':' -f2|| echo ''", returnStdout: true).trim()
  env.productVersion = sh (script : "${managingVM} ${configmap} product-version-configmap -oyaml | grep 'product-number' | cut -d '-' -f3|| echo ''", returnStdout: true).trim()
  echo "${productSetVersion}"
  echo "${productVersion}"

  if ("${deploymentType}" != '') {
    println "This cENM deployed on CHART method, please upgrade/II cENM with CSAR"
  }
}

def get_cn_build_version_info_local() {
  echo "Product Set Version: ${productSetVersion}"
  sh "curl -4 --location --request GET 'https://ci-portal.seli.wh.rnd.internal.ericsson.com/api/cloudnative/getCloudNativeProductSetContent/${productVersion}/${productSetVersion}/'>cn_confidence_level_response.json"
  def csar_index = 1
  def package_name = sh (script : "./jq '.[0] .csar_data[0].csar_name' cn_confidence_level_response.json|sed 's/\"//g'", returnStdout: true).trim()
  if ("${package_name}" == env.csar_package_name){
      csar_index = 0
  }
  echo "${csar_index}"
  env.csar_package_version=sh (script : "./jq -r '.[0] .csar_data[${csar_index}].csar_version' cn_confidence_level_response.json|sed 's/\"//g'", returnStdout: true).trim()
  if(env.csar_package_version==''){
     error("Invalid csar package provided. There is no Cloud native content for the given Product Set Version.")
  }
  env.csar_verified= sh (script : "./jq '.[0] .csar_data[${csar_index}].csar_verify' cn_confidence_level_response.json|sed 's/\"//g'", returnStdout: true).trim()
  println("csar package version":env.csar_package_version)
  println("csar verified status":env.csar_verified)  
}

def file_exist_check() {
  drop_url = "https://arm.epk.ericsson.se/artifactory/proj-enm-helm/enm-installation-package/enm-installation-package-${csar_package_version}.csar"
  internal_url = "https://arm902-eiffel004.athtem.eei.ericsson.se:8443/nexus/content/repositories/releases/cENM/csar/enm-installation-package/${csar_package_version}/enm-installation-package-${csar_package_version}.csar"
  drop_file_check = sh (script : "curl -o/dev/null -sfI ${drop_url} && echo 'OK' || echo 'NOK'", returnStdout: true).trim()
  internal_file_check = sh (script : "curl -o/dev/null -sfI ${internal_url} && echo 'OK' || echo 'NOK'", returnStdout: true).trim()
  echo "${drop_file_check}"
  echo "${internal_file_check}"
  if ( "${drop_file_check}" == 'NOK') {
    sh "echo 'cENM ${environment_name} product set version ${productSetVersion} csar not present' >> ${WORKSPACE}/csar_not_found.txt"
  }
  if ( "${internal_file_check}" == 'NOK') {
    sh "echo 'cENM ${environment_name} product set version ${productSetVersion} csar not present' >> ${WORKSPACE}/csar_not_found.txt"
  }
}

////////////////////

/*
 * Below function installs infra chart with flags enabling restore mode i.e RESTORE_STATE to ongoing, RESTORE_SCOPE to DEFAULT, RESTORE_BACKUP_NAME to backupname
 */

def restore_infra(){
    if(env.CLIENT_MACHINE_TYPE =='client_machine' && env.deployment_mechanism =='csar'){
         env.revision = sh (script : "${helm} list -n ${NAMESPACE} | grep eric-enm-infra-integration-${NAMESPACE} | awk \'{print \$(NF-7)}\'", returnStdout: true).trim()
         if (env.revision == null){
             env.revision = 0
         }
         echo "${revision}"
         if(env.product_set_version < "22.08.38"){
             sh("ssh -o LogLevel=error -o 'StrictHostKeyChecking no' ${CLIENT_MACHINE_USERNAME}@${CLIENT_MACHINE_IP_ADDRESS} 'docker run --rm -d -v ${kubeConfig}:/root/.kube/config -v ${Client_HOME}:${Client_HOME} --workdir ${Client_HOME}/conf armdocker.rnd.ericsson.se/proj-enm/cenm-build-utilities:latest helm install eric-enm-infra-integration-${NAMESPACE} --values ${Client_HOME}/cENM/Scripts/${integration_values_file_path}*  ${Client_HOME}/cENM/Definitions/OtherTemplates/eric-enm-infra-integration* --set global.restore.state=ongoing --set global.restore.scope=DEFAULT --set global.restore.backupName=$backup_name -n ${NAMESPACE} --wait --wait-for-jobs --timeout 8h'")
         }
         else{
             if(env.product_set_version < "23.06.105"){
               sh("ssh -o LogLevel=error -o 'StrictHostKeyChecking no' ${CLIENT_MACHINE_USERNAME}@${CLIENT_MACHINE_IP_ADDRESS} 'docker run --rm -d -v ${kubeConfig}:/root/.kube/config -v ${Client_HOME}:${Client_HOME} --workdir ${Client_HOME}/conf armdocker.rnd.ericsson.se/proj-enm/cenm-build-utilities:latest helm install eric-enm-infra-integration-${NAMESPACE} --values ${Client_HOME}/cENM/Scripts/${integration_values_file_path}*  ${Client_HOME}/cENM/Definitions/OtherTemplates/eric-enm-infra-integration* --set global.restore.externalStorageCredentials=${sftp_secret_name} --set global.restore.state=ongoing --set global.restore.scope=DEFAULT --set global.restore.backupName=$backup_name --set elasticsearch-bragent.brAgent.cleanRestore=true -n ${NAMESPACE} --wait --wait-for-jobs --timeout 8h'")
             }
             else{
               sh("ssh -o LogLevel=error -o 'StrictHostKeyChecking no' ${CLIENT_MACHINE_USERNAME}@${CLIENT_MACHINE_IP_ADDRESS} 'docker run --rm -d -v ${kubeConfig}:/root/.kube/config -v ${Client_HOME}:${Client_HOME} --workdir ${Client_HOME}/conf armdocker.rnd.ericsson.se/proj-enm/cenm-build-utilities:latest helm install eric-enm-infra-integration-${NAMESPACE} --values ${Client_HOME}/cENM/Scripts/${integration_values_file_path}*  ${Client_HOME}/cENM/Definitions/OtherTemplates/eric-enm-infra-integration* --set global.bro.externalStorageCredentials=${sftp_secret_name} --set global.restore.state=ongoing --set global.restore.scope=DEFAULT --set global.restore.backupName=$backup_name -n ${NAMESPACE} --wait --wait-for-jobs --timeout 8h'")
             }
         }
         sh "sleep 300s"
         sh '''
        while [[ true ]];
              do
           if [[ "$(ssh -o LogLevel=error -o 'StrictHostKeyChecking no' "${CLIENT_MACHINE_USERNAME}"@"${CLIENT_MACHINE_IP_ADDRESS}" ''\${cenmbuildutilities_client}' helm list -n '\${NAMESPACE}' | grep eric-enm-infra-integration-'\${NAMESPACE}' | awk \"{print \\\$(NF-7)}\" ')" == \$((revision+1)) && "$(ssh -o LogLevel=error -o 'StrictHostKeyChecking no' "${CLIENT_MACHINE_USERNAME}"@"${CLIENT_MACHINE_IP_ADDRESS}" ''\${cenmbuildutilities_client}' helm list --all -n '\${NAMESPACE}'|grep eric-enm-infra-integration-'\${NAMESPACE}'')" == *"deployed"* ]]; then
                         break
           elif [[ "$(ssh -o LogLevel=error -o 'StrictHostKeyChecking no' "${CLIENT_MACHINE_USERNAME}"@"${CLIENT_MACHINE_IP_ADDRESS}" ''\${cenmbuildutilities_client}' helm list -n '\${NAMESPACE}' | grep eric-enm-infra-integration-'\${NAMESPACE}' | awk \"{print \\\$(NF-7)}\" ')" == \$((revision+1)) && "$(ssh -o LogLevel=error -o 'StrictHostKeyChecking no' "${CLIENT_MACHINE_USERNAME}"@"${CLIENT_MACHINE_IP_ADDRESS}" ''\${cenmbuildutilities_client}' helm list --all -n '\${NAMESPACE}'|grep eric-enm-infra-integration-'\${NAMESPACE}'')" == *"failed"* ]]; then

                 exit 1
           else
                     logger "Waiting for infra to get to deployed ...";
                     echo "${revision}"
                     sleep 300s ;
                fi
             done
           '''
    }
    else{
        if(env.product_set_version < "22.08.38"){
            sh "${helm} install eric-enm-infra-integration-${NAMESPACE} --values cENM/Scripts/${integration_values_file_path}  cENM/Definitions/OtherTemplates/${infra_integration_chart_path} --set global.restore.state=ongoing --set global.restore.scope=DEFAULT --set global.restore.backupName=$backup_name -n ${NAMESPACE} --wait --wait-for-jobs --timeout 8h"
        }
        else{
            if(env.product_set_version < "23.06.105"){
              sh "${helm} install eric-enm-infra-integration-${NAMESPACE} --values cENM/Scripts/${integration_values_file_path}  cENM/Definitions/OtherTemplates/${infra_integration_chart_path} --set global.restore.externalStorageCredentials=${sftp_secret_name} --set global.restore.state=ongoing --set global.restore.scope=DEFAULT --set global.restore.backupName=$backup_name --set elasticsearch-bragent.brAgent.cleanRestore=true -n ${NAMESPACE} --wait --wait-for-jobs --timeout 8h"  
            }
            else{
              sh "${helm} install eric-enm-infra-integration-${NAMESPACE} --values cENM/Scripts/${integration_values_file_path}  cENM/Definitions/OtherTemplates/${infra_integration_chart_path} --set global.bro.externalStorageCredentials=${sftp_secret_name} --set global.restore.state=ongoing --set global.restore.scope=DEFAULT --set global.restore.backupName=$backup_name -n ${NAMESPACE} --wait --wait-for-jobs --timeout 8h"
            }
        }
    }
}

return this