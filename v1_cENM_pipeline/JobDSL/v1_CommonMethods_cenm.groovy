import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.yaml.*
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////  Common Generic one//////////////////////////////////////////
def extract_jq(){
    echo "Extracting the jq software"
    sh "tar -xvf software/jq-1.0.1.tar ; chmod +x ./jq"
}
def check_clusterID() {
    checkClusterIDValid = sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].name' | sed 's/\"//g'", returnStdout: true).trim()
    if(checkClusterIDValid.isEmpty()){
        throw new Exception("Environment ${environment_name} cannot be found in DIT")
    }
    env.clusterID = sh (script : "echo ${environment_name}", returnStdout: true ).trim()


}

def download_kube_config_file_from_dit(){
    env.kube_config_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cloud_native_enm_kube_config\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    env.KUBE_CRED =  sh (script : "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$kube_config_document_id\" | ./jq '.name' | sed 's/\"//g'", returnStdout: true).trim()
//     env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
//     sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
//     env.CLIENT_MACHINE_IP_ADDRESS = sh (script : "./jq '.content.global.client_machine.ipaddress' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
//     env.CLIENT_MACHINE_USERNAME = sh (script : "./jq '.content.global.client_machine.username' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
//     env.CLIENT_MACHINE_TYPE = sh (script : "./jq '.content.global.client_machine.type' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
//     env.NAMESPACE =  sh (script : "./jq '.content.global.namespace' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    sh "rm -rf ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    sh "mkdir -p ${workspace}/Kube-Config-Files/"
    sh "touch ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    sh "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$kube_config_document_id\" | ./jq '.content' | sed 's/\"//g' >> ${workspace}/Kube-Config-Files/${KUBE_CRED}"
    env.kubeConfig = "${workspace}/.kube/${KUBE_CRED}"
//     env.helm = "docker run --rm -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} helm"
//     env.kubectl = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl"
//     env.brocli = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl exec deploy/brocli -n ${env.NAMESPACE} -- brocli"
//     env.troubleshooting = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl exec deploy/troubleshooting-utils -n ${env.NAMESPACE} --"
//     env.configmap = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl get cm -n ${env.NAMESPACE}"
    //env.managingVM = "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${managing_vm}"
}
def set_kube_config_file(){
    sh 'mkdir -p ${PWD}/.kube && chmod 775 ${PWD}/.kube && cp -v ${PWD}/Kube-Config-Files/${KUBE_CRED} ${PWD}/.kube/${KUBE_CRED} && chmod 620 ${PWD}/.kube/${KUBE_CRED}'
}
def domainIPGet() {
  env.backupserverip_seli = sh (script : "cat ${WORKSPACE}/burInventory.csv| grep ${env.clusterID}| cut -d \",\" -f3", returnStdout: true).trim()
  env.backupserverip_sero = sh (script : "cat ${WORKSPACE}/burInventory.csv| grep ${env.clusterID}| cut -d \",\" -f4", returnStdout: true).trim()
}
def load_kubeconfig_from_credentials() {
    def kubeConfigCredFile = env.KUBECONFIG
    sh "install -m 664 ${kubeConfigCredFile} ${PWD}/admin.conf"
    env.kubeConfig_cr = "${PWD}/admin.conf"
    kubectl = "docker run --rm  -v ${kubeConfig_cr}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl"
    kube_result = kube_config_integrity_check("${kubectl}")
    if( "${kube_result}" == "OK" ){
        echo "Integrity check for Kubeconfig from Credentials store completed with success. Proceeding to next stages"
        sh 'mkdir -p ${PWD}/.kube && chmod 775 ${PWD}/.kube && cp -v ${kubeConfig_cr} ${PWD}/.kube/${environment_name}_kubeconfig.conf && chmod 620 ${PWD}/.kube/${environment_name}_kubeconfig.conf'
        sh 'sleep 5 && rm -rf ${kubeConfig_cr}'
        env.KUBECONFIG_FROM_CREDENTIALS = "Yes"
    }else{
        env.KUBECONFIG_FROM_CREDENTIALS = "No"
        error('Integrity check for Kubeconfig from Credentials store Failed. Proceeding with DIT fetch in next stage')
    }
}
def set_site_info_env() {
    /// Setting Kubeconfig from Credentials flag as No for all Deployment types
    env.KUBECONFIG_FROM_CREDENTIALS = "No"
    /// Site information related for all Deployment types
    env.site_information_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_site_information\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$site_information_document_id'>deployment_site_config_information.json"
    env.CLIENT_MACHINE_IP_ADDRESS = sh (script : "./jq '.content.global.client_machine.ipaddress' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CLIENT_MACHINE_USERNAME = sh (script : "./jq '.content.global.client_machine.username' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CLIENT_MACHINE_TYPE = sh (script : "./jq '.content.global.client_machine.type' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.NAMESPACE =  sh (script : "./jq '.content.global.namespace' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
}
def set_k8s_env() {
    /// Kubeconfig file related
    if(fileExists("${workspace}/.kube/${KUBE_CRED}") && "${KUBE_CRED}" != "${environment_name}_kubeconfig.conf"){
        sh "mv -nv ${workspace}/.kube/${KUBE_CRED} ${workspace}/.kube/${environment_name}_kubeconfig.conf"
    }else{
        echo "File at path ${workspace}/.kube/${KUBE_CRED} having the same name"
    }
    env.kubeConfig = "${workspace}/.kube/${environment_name}_kubeconfig.conf"
    env.helm = "docker run --rm -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} helm"
    env.kubectl = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl"
    env.brocli = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl exec deploy/brocli -n ${env.NAMESPACE} -- brocli"
    env.troubleshooting = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl exec deploy/troubleshooting-utils -n ${env.NAMESPACE} --"
    env.configmap = "docker run --rm  -v ${kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl get cm -n ${env.NAMESPACE}"
}
/////////////////////////////////// Docker login
def docker_login_test(username,IP,registry) {
  docker_login_result = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' $username@$IP docker login $registry -u admin -p dets123 > /dev/null 2>&1 && echo \"OK\" || echo \"NOK\"", returnStdout: true).trim()
  return docker_login_result
}
def docker_login_check(username,IP) {
    /*if ("${Deployment}" != "Flexikube"){
        ssh_options = "-o 'LogLevel=error' -o 'StrictHostKeyChecking no'"
        if ("${Deployment}" == "NFVI"){
        ca_cert_path="/etc/pki/trust/anchors/ca.crt"
        } else {
        ca_cert_path="/etc/kubernetes/pki/ca.crt"
        }*/
    TARGET_DOCKER_REGISTRY_URL = sh (script : "./jq '.content.global.registry.hostname' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CONTAINER_REGISTRY_USERNAME = sh (script : "./jq '.content.global.registry.users.username' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    env.CONTAINER_REGISTRY_PASSWORD = sh (script : "./jq '.content.global.registry.users.password' deployment_site_config_information.json| sed 's/\"//g'", returnStdout: true).trim()
    docker_login_result = sh (script : "ssh -o LogLevel=error -o 'StrictHostKeyChecking no' $username@$IP docker login ${TARGET_DOCKER_REGISTRY_URL} -u ${CONTAINER_REGISTRY_USERNAME} -p ${CONTAINER_REGISTRY_PASSWORD} > /dev/null 2>&1 && echo \"OK\" || echo \"NOK\"", returnStdout: true).trim()
    echo "${docker_login_result}"
    if("${docker_login_result}" == "NOK"){
        //mvcm_IP = cvmIpValidationCheck()
        echo "Docker login is failed running script to transfer cert"
        // sh "ssh -o 'LogLevel=error' -o 'StrictHostKeyChecking no' cenmbuild@${mvcm_IP} bash +x /home/cenmbuild/AUTO/script/kubeCertCopyFromCcd.sh ${env.clusterID}"
        //dir_ip = sh (script : "cat ${WORKSPACE}/inventory.csv | grep ${env.clusterID} | cut -d ',' -f3", returnStdout: true).trim()
        //reg_url = sh (script : "cat ${WORKSPACE}/inventory.csv | grep ${env.clusterID} | cut -d ',' -f5", returnStdout: true).trim()
        //echo "Director/master node IP: ${dir_ip}"
        //echo "Docker Registry URL: ${reg_url}"
        //sh "scp -r ${ssh_options} ${WORKSPACE}/v1_cENM_pipeline/script/ansible_main cenmbuild@${mvcm_IP}:/home/cenmbuild/AUTO"
        sh "ansible-playbook -i v1_cENM_pipeline/script/ansible_main/cenm_inventory v1_cENM_pipeline/script/ansible_main/get_k8s_certs.yaml -e \"file_path=${WORKSPACE}\" -e \"ansible_password=${mcvm_pass}\""
        result_again = sh (script : "ssh -o 'LogLevel=error' -o 'StrictHostKeyChecking no' $username@$IP docker login ${TARGET_DOCKER_REGISTRY_URL} -u ${CONTAINER_REGISTRY_USERNAME} -p ${CONTAINER_REGISTRY_PASSWORD} > /dev/null 2>&1 && echo \"OK\" || echo \"NOK\"", returnStdout: true).trim()
        if( "${result_again}" == "NOK" ){
            error('______Failed: Something is nok transfer correct certificate manually to make docker login')
        }else{
            println('#### INFO: Docker Login Succeeded from the client machine. Proceeding to the next stage')
        }
    }else{
        println('#### INFO: Docker Login Succeeded from the client machine. Proceeding to the next stage')
    }
}
///////////////////// master client machine selection wrt passwordless login and liveness
def master_cvm_connection_check(){
  def list_of_cvm = ['214.14.16.20', '214.14.16.36', '214.5.198.30']
  //def list_of_cvm = ['214.14.16.204', '214.5.198.44', '214.14.16.436', '214.5.198.1948']
  for(def cvm:list_of_cvm){
    def loginCheck = sh (script : "timeout 7s ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@${cvm} echo > /dev/null 2>&1 && echo \"Success\" || echo \"No Connection\"", returnStdout: true).trim()
    if( "${loginCheck}" == 'Success'){
      return cvm
    }else{
      echo "______Warning: master client machine IPs ${cvm} is not accessible from Jenkinks agent server, checking other vm"
    }
  }
}
def cvmIpValidationCheck(){
  mcvm = master_cvm_connection_check()
  if("${mcvm}" == 'null'){
    echo "______ERROR: No master client IP from the pool is reachable from jenkins server"
    echo "______ERROR: Use manual or contact ekasviv for more troubleshooting"
    error('______Failed: Exiting the Job as this is much needed requirment')
  }else{
    return mcvm
  }
}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////  client machine update section ///////////////////////////
///////////////////// Display clientmachine IP from DIT
def countCheckfromList(){
    totalcENMcount = sh (script : "cat ${WORKSPACE}/inventory.csv| grep cENM | wc -l", returnStdout: true).trim()
    echo "Total Number of cENM present in the list - ${totalcENMcount}"
    int INTtotalcENMcount = Integer.parseInt(totalcENMcount)
    return INTtotalcENMcount
}
def clientvmlist(){
  if( "${environment_name}" == "ALL"){
    totalCount = countCheckfromList()
    for(int i=1;i<=totalCount;i++){
    env.deploymentName = sh (script : "cat ${WORKSPACE}/inventory.csv | grep cENM| cut -d ',' -f1 | awk '(NR==$i)'", returnStdout: true).trim()
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
/////////////////////////// client machine IP update on DIT including pre check
def passwordless_login_test_from_Jenkins_to_cvm(clientMachine) {
  loginCheck = sh (script : "timeout 7s ssh -o LogLevel=error -o 'StrictHostKeyChecking no' cenmbuild@$clientMachine echo > /dev/null 2>&1 && echo \"Success\" || echo \"No Connection\"", returnStdout: true).trim()
  return loginCheck
}
def cvm_update_pre_check() {
  result = passwordless_login_test_from_Jenkins_to_cvm("${clientMachineIP}")
  echo "${result}"
  if( "${result}" != "Success" ){
    error("client machine ${clientMachineIP} is not up and running 'or' passwordless connection from jenkins agent to the client machine is not set, update the private key")
  }
  //docker login test
  docker_login_check("cenmbuild","${clientMachineIP}")
}
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
////////////////////////////////////////////  pre-deploy cENM ///////////////////////////
/////////////////////  DIT kube config file validation check and correction
def download_kube_config_file_from_dit_to_make_the_validation(){
    env.D_kube_config_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cloud_native_enm_kube_config\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    KUBE_CRED =  sh (script : "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$D_kube_config_document_id\" | ./jq '.name' | sed 's/\"//g'", returnStdout: true).trim()
    sh "rm -rf ${workspace}/DIT_Kube_Config_File/"
    sh "mkdir -p ${workspace}/DIT_Kube_Config_File/"
    sh "mkdir -p ${workspace}/Kube-Config-Files/"
    sh "touch ${workspace}/DIT_Kube_Config_File/${KUBE_CRED}"
    env.D_kubeConfig = "${workspace}/DIT_Kube_Config_File/${KUBE_CRED}"
    sh "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$D_kube_config_document_id\" | ./jq '.content' | sed 's/\"//g' >> ${env.D_kubeConfig}"
    env.D_kubectl = "docker run --rm  -v ${D_kubeConfig}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl"
}
def kube_config_integrity_check(kubeCommand) {
    dit_kube_result = sh (script : "$kubeCommand get ns > /dev/null 2>&1 && echo \"OK\" || echo \"NOK\"", returnStdout: true).trim()
    return dit_kube_result
}
def kube_config_file_integrity_check() {
    download_kube_config_file_from_dit_to_make_the_validation()
    dit_kube_result = kube_config_integrity_check("${env.D_kubectl}")
    if( "${dit_kube_result}" == "NOK" ){
        // script to copy the kube file and update with correct value.
        sh "bash ${WORKSPACE}/v1_cENM_pipeline/script/copyKubeFile.sh ${env.clusterID} ${ccd_client_master_key}"
        // new copied kubeconfig file from CCD to local jenkins server
        env.kubeConfig_l = "${workspace}/org_kube_file/${env.clusterID}_kubeFile"
        env.kubectl_l = "docker run --rm  -v ${kubeConfig_l}:/root/.kube/config -v ${WORKSPACE}:${WORKSPACE} --workdir ${WORKSPACE} ${cenm_utilities_docker_image} kubectl"
        dit_kube_result_1 = kube_config_integrity_check("${env.kubectl_l}")
        if( "${dit_kube_result}" == "OK" ){
            echo "proceding to upload the file into DIT"
            //updateDIT()
        }else{
            sh "cat ${workspace}/org_kube_file/${env.clusterID}_kubeFile"
            error('check the file downloaded from CCD server')
        }
    }else{
        echo "_____INFO: kube config file present in the DIT looks fine proceeding to next stage"
    }
}
def updateDIT() {
    //gets template from dit, will need to update content of this
    //def testConfig = sh (script : "curl -X GET https://atvdit.athtem.eei.ericsson.se/api/documents/${kubeConfigID}",returnStdout: true).trim()
    //writeFile file: 'kubeconfig/json_get_config', text: testConfig
    sh "rm -rf ${workspace}/DIT_kubeConfig_document"
    sh "touch ${workspace}/DIT_kubeConfig_document"
    sh "curl -4 -s \"https://atvdit.athtem.eei.ericsson.se/api/documents/$env.D_kube_config_document_id\" >> ${workspace}/DIT_kubeConfig_document"
    //converts standard kube config file to json
    def inputFile = readYaml file: "${workspace}/org_kube_file/${env.clusterID}_kubeFile"
    def json = new JsonBuilder(inputFile).toPrettyString()
    writeFile file: "${workspace}/org_kube_file/${env.clusterID}_kubeFile.json", text: json
    //reads template into file variable
    jsonfile = readJSON file: "${workspace}/DIT_kubeConfig_document"
    kubejsoncontent = readJSON file: "${workspace}/org_kube_file/${env.clusterID}_kubeFile.json"
    jsonfile['content'] = kubejsoncontent
    writeFile file: "${workspace}/org_kube_file/validated_${env.clusterID}_kubeFile.json", text: jsonfile.toString()
}
def upload_kube_to_dit(){
    if(fileExists("${workspace}/org_kube_file/validated_${env.clusterID}_kubeFile.json")){
        echo "uploading the file"
        echo "${detsusername}"
        echo "${detspassword}"
        sh "curl -u '${detsusername}:${detspassword}' -X PUT 'https://atvdit.athtem.eei.ericsson.se/api/documents/$env.D_kube_config_document_id' -H 'accept: application/json' -H 'Content-Type: application/json' -d @${workspace}/org_kube_file/validated_${env.clusterID}_kubeFile.json"
        sh "sleep 2"
    }else{
        echo "File at path ${workspace}/org_kube_file/validated_${env.clusterID}_kubeFile.json cannot be found"
    }
    //echo "${detsusername}"
    //echo "${detspassword}"
    //sh "curl -u '${detsusername}:${detspassword}' -X PUT 'https://atvdit.athtem.eei.ericsson.se/api/documents/$env.kube_config_document_id' -H 'accept: application/json' -H 'Content-Type: application/json' -d @${workspace}/org_kube_file/v1_${clusterID}_kubeFile.json"
}
/////////////////////////// Jenkins to client machine connection check
def connection_check(){

    if(env.CLIENT_MACHINE_TYPE == "slave"){
        echo "No client vm attached to this deployment, please check cENM_site_information in DIT if this is incorrect"
    }else{

        result = sh (script : "ssh -o 'LogLevel=error' -o 'StrictHostKeyChecking no' ${CLIENT_MACHINE_USERNAME}@${CLIENT_MACHINE_IP_ADDRESS} hostname > /dev/null 2>&1 && echo \"OK\" || echo \"NOK\"", returnStdout: true).trim()
        if("${result}" == "NOK"){
            error('Jenkins public key is not present in the client machine or client is not up and running')
        }else{
            println("#### INFO: Passwordless login to clientmachine from jenkins server is working fine continuning to next step")
        }
    }
}
/////////////////////////// Exclude CIDR for cENM
def excludeCIDR(){
    null_value_check = sh (script: "${env.kubectl} get cm kube-proxy -n kube-system -oyaml | grep excludeCIDRs | awk '{print \$2}'", returnStdout: true).trim()
    if("${null_value_check}" == "null"){
        error("WARNING: please add ENM service VLAN to into kube-proxy configmap")
    }else{
        println('cENM Service VLAN IP Network present in the configmap under excludeCIDR')
        println('###################################')
        sh "${env.kubectl} get cm kube-proxy -n kube-system -oyaml | grep excludeCIDRs -A2"
        println('###################################')
    }
}
///////////////////////// CCD level version display
def ccdLvlVersionCheck(){
    env.masterOamIP = sh (script: "cat ${workspace}/inventory.csv| grep ${env.clusterID} | cut -d ',' -f3 ", returnStdout: true).trim()
    println("#### INFO: CCD version")
    if ("${Deployment}" == "Flexikube"){
        def ccdversion = sh (script: "${env.kubectl} describe nodes | grep 'ccd/version'", returnStdout: true).trim()
        println("${ccdversion}")
    }else{
        sh "ssh -i ${ccd_client_master_key} -o 'LogLevel=error' -o 'StrictHostKeyChecking no' eccd@${masterOamIP} cat /etc/eccd/eccd_image_version.ini"
        // Validating Container run time version
        println("#### INFO: Container Runtime version")
        sh "ssh -i ${ccd_client_master_key} -o 'LogLevel=error' -o 'StrictHostKeyChecking no' eccd@${masterOamIP} \"sudo docker version | grep Version || sudo crictl version | grep Version\""
    }
    println("#### INFO: Kubernetes version")
    sh "${env.kubectl} version --short --client"
    println("#### INFO: cluster info")
    sh "${env.kubectl} cluster-info"
    println("#### INFO: total nodes")
    sh "${env.kubectl} get nodes"
    println("#### INFO: Helm version check")
    sh "${env.kubectl} version --client --short"
}
////////////////////// Namespace creation
def nameSpaceCreate(){
    cenm_namespaceCheck = sh (script : "${env.kubectl} get ns | grep ${env.NAMESPACE}||echo ''",returnStdout: true ).trim()
    crd_namespaceCheck = sh (script : "${env.kubectl} get ns | grep eric-crd-ns ||echo ''",returnStdout: true ).trim()
    if (cenm_namespaceCheck == "") {
      println("namespace need to create as per DIT value")
      sh "${env.kubectl} create ns  ${env.NAMESPACE}"
      sh "sleep 1"
      sh "${env.kubectl} get ns"
    }
    else {
      println("INFO: ####### ${env.NAMESPACE} Namespace already present continuing to the next stage")
    }
    if (crd_namespaceCheck == "") {
      println("\'eric-crd-ns\' namespace not present, make sure redis cert should be present")
    }
    else {
      println("INFO: ####### \'eric-crd-ns\' Namespace already present continuing to the next stage")
    }
}
//////////////////// SNMP UI workaround
def createInventoryFile(){
    //remove inventory file if exists
    def remove_inventory_file = sh (script : "${flexidirectorNodeSSH} sudo rm /home/${flexiclusterOwnerSignum}/inventory || true", returnStdout: true).trim()
    echo "${remove_inventory_file}"
    //generate new inventory file with up-to-date node names
    def create_inventory_file = sh (script : "${flexidirectorNodeSSH} \"kubectl get node -o wide | awk '{print \\\$1,\\\"\\\",\\\"ansible_host=\\\"\\\$6}' > inventory\"", returnStdout: true).trim()
    echo "${create_inventory_file}"
    def inventory_file_output = sh (script : "${flexidirectorNodeSSH} cat /home/${flexiclusterOwnerSignum}/inventory", returnStdout: true).trim()
    echo "Newly creatd inventory file contents:\n${inventory_file_output}"
    //update the inventory file with the required parameters
    def update_inventory_file1 = sh (script : "${flexidirectorNodeSSH} sed -i '2i\\[master]' inventory", returnStdout: true).trim()
    def update_inventory_file2 = sh (script : "${flexidirectorNodeSSH} sed -i '6i\\[workers:children]\\\\n\\worker\\\\n[worker]' inventory", returnStdout: true).trim()
    def update_inventory_file3 = sh (script : "${flexidirectorNodeSSH} \"printf \\\"[all:vars]\\\" >> inventory\"", returnStdout: true).trim()
    def update_inventory_file4 = sh (script : "${flexidirectorNodeSSH} \"printf \\\"\\\\nansible_python_interpreter=/usr/bin/python3\\\" >> inventory\"", returnStdout: true).trim()
    def update_inventory_file5 = sh (script : "${flexidirectorNodeSSH} \"printf \\\"\\\\nansible_ssh_common_args='-o StrictHostKeyChecking=no'\\\\n\\\" >> inventory\"", returnStdout: true).trim()
    def inventory_file_output2 = sh (script : "${flexidirectorNodeSSH} cat /home/${flexiclusterOwnerSignum}/inventory", returnStdout: true).trim()
    echo "Updated inventory file contents:\n${inventory_file_output2}"
}
def SNMP_and_UI_WA_FlexiKube(){
    echo "SNMP & UI WA"
    def parameter = "ansible worker -b -i inventory -m shell -a \"sysctl net.ipv4.conf.all.rp_filter;sysctl net.ipv4.conf.tunl0.rp_filter;sysctl net.ipv4.vs.conntrack\""
    def parameter_check = sh (script : "${flexidirectorNodeSSH} '${parameter}'", returnStdout: true).trim()
    echo "SYSCTL PARAMETERS BEFORE UPDATE:\n${parameter_check}"
    if (parameter_check.contains("net.ipv4.conf.all.rp_filter = 1")){
        def parameter_update_1_command = "ansible worker -b -i inventory -m shell -a \"echo -e 'net.ipv4.conf.all.rp_filter=0'>>/etc/sysctl.conf && sysctl -p\""
        def parameter_update_1 = sh (script : "${flexidirectorNodeSSH} '${parameter_update_1_command}'", returnStdout: true).trim()
        echo "UPDATING: net.ipv4.conf.all.rp_filter from 1 to 0 on all worker nodes"
        echo "${parameter_update_1}"
    }
    else
        echo "net.ipv4.conf.all.rp_filter is already set to 0 on all worker nodes"
    if (parameter_check.contains("net.ipv4.conf.tunl0.rp_filter = 1")) {
        def parameter_update_2_command = "ansible worker -b -i inventory -m shell -a \"echo -e 'net.ipv4.conf.tunl0.rp_filter=0'>>/etc/sysctl.conf && sysctl -p\""
        def parameter_update_2 = sh (script : "${flexidirectorNodeSSH} '${parameter_update_2_command}'", returnStdout: true).trim()
        echo "UPDATING: net.ipv4.conf.tunl0.rp_filter from 1 to 0 on all worker nodes"
        echo "${parameter_update_2}"
    }
    else
        echo "net.ipv4.conf.tunl0.rp_filter is already set to 0 on all worker nodes"
    if (parameter_check.contains("net.ipv4.vs.conntrack = 0")){
        def parameter_update_3_command = "ansible worker -b -i inventory -m shell -a \"echo -e 'net.ipv4.vs.conntrack=1'>>/etc/sysctl.conf && sysctl -p\""
        def parameter_update_3 = sh (script : "${flexidirectorNodeSSH} '${parameter_update_3_command}'", returnStdout: true).trim()
        echo "UPDATING: net.ipv4.vs.conntrack from 0 to 1 on all worker nodes"
        echo "${parameter_update_3}"}
    else
        echo "net.ipv4.vs.conntrack is already set to 1 on all worker nodes"
    //Checking the net.ipv4.conf.all.rp_filter, net.ipv4.conf.tunl0.rp_filter and net.ipv4.vs.conntrack parameters and saving to file
    def parameter_check2 = sh (script : "${flexidirectorNodeSSH} \"\"\"ansible worker -b -i inventory -m shell -a 'sysctl net.ipv4.conf.all.rp_filter;sysctl net.ipv4.conf.tunl0.rp_filter;sysctl net.ipv4.vs.conntrack'\"\"\"", returnStdout: true).trim()
}
def snmpUI(){
    if("${Deployment}" == "Flexikube"){
        createInventoryFile()
        SNMP_and_UI_WA_FlexiKube()
    }else{
        ssh_options = "-o 'LogLevel=error' -o 'StrictHostKeyChecking no'"
        sh "ssh -i ${ccd_client_master_key} ${ssh_options} eccd@${env.masterOamIP} 'bash -s ' < ${workspace}/v1_cENM_pipeline/script/snmpUI.sh"
        sh "scp -r -i ${ccd_client_master_key} ${ssh_options} eccd@${env.masterOamIP}:~/sysconf_backups.tar.gz ${workspace}"
        // Remove sysconf backups from masterOam node
        sh "ssh -i ${ccd_client_master_key} ${ssh_options} eccd@${env.masterOamIP} 'rm -rf ~/sysconf_backups.tar.gz'"
    }
}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////// Post deploy cENM ///////////////////////////////////////////////////////
def ingressCert(){
    managing_vm = cvmIpValidationCheck()
    println "###################################### ${environment_name} Ingress certificate tls enm ui ca ####################################"
    sh "ssh cenmbuild@${managing_vm} 'bash /home/cenmbuild/AUTO/script/remmotescript.sh ${env.clusterID} ${env.NAMESPACE}'"
    echo "################################################### TLS cert task ENDs HERE #########################################################"
}
def smokeTest() {
    env.integration_value_file_document_id= sh (script : "curl -4 -s \"http://atvdit.athtem.eei.ericsson.se/api/deployments/?q=name=$environment_name\" |./jq '.[].documents[] | select(.schema_name==\"cENM_integration_values\") | .document_id' | sed 's/\"//g'", returnStdout: true).trim()
    sh "curl -4 --location --request GET 'https://atvdit.athtem.eei.ericsson.se/api/documents/$integration_value_file_document_id'>deployment_integration_values_file.json"
    env.ENM_LAUNCHER_HOSTNAME= sh (script : "./jq '.content.global.ingress.enmHost' deployment_integration_values_file.json| sed 's/\"//g'", returnStdout: true).trim()
    response = sh (script: "curl -4 --insecure -X POST -d \"IDToken1=Administrator&IDToken2=TestPassw0rd\" https://${ENM_LAUNCHER_HOSTNAME}/login -H \"Content-Type: application/x-www-form-urlencoded\" -H \"Accept-Encoding: gzip,deflate\" -H \"Accept: */*\" -L -H \'cache-control: no-cache\'|| echo \'\'", returnStdout: true ).trim()
    echo response
    if ( response.contains("Authentication Successful") ){
       echo "#### Success! Can login to ENM"
    } else {
       echo "_____Failed! Can\'t login to ENM____"
       sh "exit 1"
    }
}
def backupHealthCheck() {
    healthOutput = sh (script: "${env.brocli} status | grep Health | awk '{print \$2}'", returnStdout: true).trim()
    echo "${healthOutput}"
    if ( "${healthOutput}" == "Healthy") {
      echo "#### brocli is in Healthy state"
    } else {
      echo "____brocli state is unhealthy!! NOK"
      sh "exit 1"
    }
}
def domainCheck(){
  if( "${Domain}" == "SELI" ){
    env.sftp_url = "sftp://cnsftpuser@${env.backupserverip_seli}:22/${env.clusterID}/"
  }else{
    println "SERO server being choose here"
  }
  if( "${Domain}" == "SERO"){
    env.sftp_url = "sftp://cnsftpuser@${env.backupserverip_sero}:22/${env.clusterID}/"
  }else{
    println "SELI server being choose here"
  }
}
def backupConfigurePreTask() {
    env.backupTime = sh (script : "cat ${WORKSPACE}/burInventory.csv | grep ${env.clusterID}| cut -d \",\" -f2", returnStdout: true).trim()
    env.scheduleCheck = sh (script : "${env.brocli} schedule | grep \"Export URI\" | awk '{print \$3}'", returnStdout: true).trim()
    env.scheduleInterval = sh (script : "${env.brocli} schedule | grep \"Schedule intervals for scope DEFAULT\"|| echo ''", returnStdout: true).trim()
    env.retensionCheckDefault = sh (script : "${env.brocli} retention show --scope DEFAULT| grep \"Backup Limit\" | awk '{print \$3}'", returnStdout: true).trim()
    env.exportPrefix = sh (script : "${env.brocli} schedule | grep \"Backup Prefix\" | awk '{print \$3}'", returnStdout: true).trim()
    echo "${env.backupTime}"
    echo "${env.scheduleCheck}"
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
      sh "${env.brocli} retention set --limit 2 --purge --scope DEFAULT"
    } else {
      println("#### Retension period is upto STC norms")
    }
}
def backupScheduling() {
    if("${Deployment}" == "Flexikube"){
        if ("${product_set_version}" != "0.0.0") {
            cenm_version = "${product_set_version}"
        } else {
            cenm_version = sh (script : "${env.kubectl} get namespace ${env.NAMESPACE} -o=jsonpath='{.metadata.annotations.cenm-product-set-version}'", returnStdout: true).trim()
        }
    } else {
        cenm_version = sh (script : "${env.configmap} eric-enm-version-configmap -oyaml | grep product-set-version | cut -d ':' -f2", returnStdout: true).trim()
    }
    echo "${cenm_version}"
    sh "${env.brocli} schedule set --${schedulingTask} --prefix ${env.clusterID}cenm_${cenm_version} --export --export-uri ${env.sftp_url} --export-password DefaultP12345"
}
def addscheduleInterval() {
    dateStamp = sh (script : "date +'%F' -d '1 days'", returnStdout: true).trim()
    if ("${env.scheduleInterval}" == ""){
      echo "#### Need to set one interval which will take backup in weekly basis"
      sh "${env.brocli} schedule add --every 1w --start ${dateStamp}T${env.backupTime}:00"
      echo "#################### ${environment_name} #########################"
      sh "${env.brocli} schedule"
    } else {
      println "#### One Interval Already set, For more new Interval, run manually or remove existing one"
    }
  }
//////////////////////flexikube setup//////////////////////////////
def readHydraInformation() {
    productionToken = "d258d9d946c771448b8de590f735b90e309b56a9"
    hydra = "curl -k -s -X GET -H 'Authorization: ${productionToken}'"
    // Get Instance ID from cluster name
    instanceID = sh (script : "${hydra} https://hydra.gic.ericsson.se/api/8.0/instance?name=${environment_name} | ./jq '.result[0].id'",returnStdout: true).trim()
    //echo "Director CPU Total = ${directorCPU}"
    def CustomFieldIds = ["OwnerSignum" : ["12978",""],
                          "DirectorNodes" : ["14271",""]]
    for (key in CustomFieldIds.keySet()){
        //CustomFieldIds[key][1] = sh (script : "curl -k -s -X GET -H 'Authorization: ${productionToken}' ${CustomFieldIds[key][0]} | ./jq '.result[0].data'",returnStdout: true).trim()
        CustomFieldIds[key][1] = sh (script : "curl -k -s -X GET -H 'Authorization: ${productionToken}' https://hydra.gic.ericsson.se/api/8.0/instance_custom_data?instance_id=${instanceID}\\&custom_field_id=${CustomFieldIds[key][0]} | ./jq '.result[0].data'",returnStdout: true).trim()
        echo "${key} = ${CustomFieldIds[key][1]}"
    }
    def directorNodeIP = readJSON text: CustomFieldIds["DirectorNodes"][1].substring(1, CustomFieldIds["DirectorNodes"][1].size() - 1)
    echo "Director Node IP (as per HYDRA) is ${directorNodeIP[0].dir_oam_ip}"
    //Use this for the cluster director node commands
    env.flexidirectorNodeSSH = "sshpass -p ${CustomFieldIds['OwnerSignum'][1]} ssh -o LogLevel=error -o 'StrictHostKeyChecking=no' ${CustomFieldIds['OwnerSignum'][1]}@${directorNodeIP[0].dir_oam_ip}"
    //Used for copying the kubeconfig file from the director node of the cluster to the workspace folder of the Jenkins slave
    env.flexiclusterOwnerSignum = "${CustomFieldIds['OwnerSignum'][1]}"
    env.flexidirectorIP = "${directorNodeIP[0].dir_oam_ip}"
}
////////////////////////////////////////////////////
return this