

def master_cvm_connection_check(){
  def list_of_cvm = ['214.14.16.20', '214.5.198.4', '214.14.16.36', '214.5.198.198']
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


def copy_inventory_file(){
    env.clientMachine = cvmIpValidationCheck()
    // copy inventory file
    sh "scp cenmbuild@${clientMachine}:/home/cenmbuild/AUTO/ansible/inventory ${WORKSPACE}/Ansible/JobDsl/"
}

def confirm_inventory_file(){
   copy_inventory_file()
   if(fileExists("${WORKSPACE}/Ansible/JobDsl/inventory")){
    echo "Inventory file present to run ansible command"
   }else {
    error('File not copied properly from cvm to Jenkins Agent server so exiting the job')
   }
}

def cert_update_on_cvm(){
  if( "${}" == "All"){
    sh "ssh cenmbuild@${env.clientMachine}"

  }else {
    echo "skip"
  }
}

return this