def commonMethods
def v1_CommonMethods_cenm
pipeline{
    agent {
            node
            {
               label slave
            }
        }
    environment {
            HOME_DIR = "${WORKSPACE}"
            cenm_utilities_docker_image = "armdocker.rnd.ericsson.se/proj-enm/cenm-build-utilities:latest"
            nexus_repositoryUrl = "https://arm902-eiffel004.athtem.eei.ericsson.se:8443/nexus/content/repositories/releases/"
            helm_repository_release = "https://arm.seli.gic.ericsson.se/artifactory/proj-enm-helm/"
            helm_repository_ci_internal = "https://arm.seli.gic.ericsson.se/artifactory/proj-enm-dev-internal-helm/"
            csar_package_name = "enm-installation-package"
            Client_HOME = "/home/cenmbuild"
    }
    stages{
        stage('Clean Up WorkSpace'){
            steps{
                deleteDir()
            }
        }
        stage('Checkout dets_automation Pipeline Git Repository') {
            steps {
                git branch: '${BRANCH_NAME}',
                        url: 'ssh://gerrit.ericsson.se:29418/DETES/com.ericsson.de.stsoss/dets_automation_pipeline'
                sh '''
                    git remote set-url origin --push ssh://gerrit.ericsson.se:29418/DETES/com.ericsson.de.stsoss/dets_automation_pipeline
                '''
                clone_ci_repo()
            }
        }
        stage('Load common methods') {
            steps {
                script {
                    commonMethods = load("${env.WORKSPACE}/eric-enm-integration-pipeline-code/Jenkins/JobDSL/CommonMethods.groovy")
                    v1_CommonMethods_cenm = load("${env.WORKSPACE}/v1_cENM_pipeline/JobDSL/v1_CommonMethods_cenm.groovy")
                }
            }
        }
        stage('Delete docker image') {
            steps {
                script {
                    commonMethods.delete_build_utilities_image()
                }
            }
        }
        stage('Pull docker image') {
            steps {
                script {
                    sh "docker pull ${cenm_utilities_docker_image}"
                }
            }
        }
        stage('try managed files') {
            steps {
                configFileProvider([configFile(fileId: "inventory.csv", targetLocation: "${env.WORKSPACE}"),configFile(fileId: "burInventory.csv", targetLocation: "${env.WORKSPACE}")]) {
                    script {
                        sh "pwd"
                        sh "ls -latr"
                    }
                }
            }
        }
        stage( 'Pre Configurations' ) {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'ccd_client_master_key', keyFileVariable: 'ccd_client_master_key'),
                    usernamePassword(credentialsId: 'detsFunUser', passwordVariable: 'detspassword', usernameVariable: 'detsusername'),
                    string(credentialsId: 'stsoss_client_vm_pass', variable: 'mcvm_pass')
                    ]) {
                    configFileProvider([configFile(fileId: "burInventory.csv", targetLocation: "${env.WORKSPACE}")]) {}
                    configFileProvider([configFile(fileId: "inventory.csv", targetLocation: "${env.WORKSPACE}")]) {}
                    script{
                        v1_CommonMethods_cenm.extract_jq()
                        try{
                            v1_CommonMethods_cenm.check_clusterID()
                        }catch(error){
                            echo "${error}"
                            throw error
                        }
                        if("${Deployment}" == "Flexikube"){
                            v1_CommonMethods_cenm.readHydraInformation()
                        }
                        v1_CommonMethods_cenm.set_site_info_env()
                        v1_CommonMethods_cenm.kube_config_file_integrity_check()
                        v1_CommonMethods_cenm.upload_kube_to_dit()
                        v1_CommonMethods_cenm.download_kube_config_file_from_dit()
                        commonMethods.set_kube_config_file()
                        v1_CommonMethods_cenm.set_k8s_env()
                    }
                }
            }
        }
        stage('docker login check from client machine') {
            when {
                environment name: 'method', value: 'csar'
            }
            steps {
                catchError(stageResult: 'FAILURE') {
                    withCredentials([string(credentialsId: 'stsoss_client_vm_pass', variable: 'mcvm_pass')]) {
                        configFileProvider([configFile(fileId: "dockerRegistryInsecure", targetLocation: "${env.WORKSPACE}")]) {}
                        script {
                            v1_CommonMethods_cenm.docker_login_check("${env.CLIENT_MACHINE_USERNAME}","${env.CLIENT_MACHINE_IP_ADDRESS}")
                        }
                    }
                }
            }
        }
        stage('passwordless connection check from Jenkins to client machine'){
            steps {
                catchError(stageResult: 'FAILURE') {
                    script{
                        v1_CommonMethods_cenm.connection_check()
                    }
                }
            }
        }
        stage('Exclude CIDR check for cENM'){
            steps {
                catchError(stageResult: 'FAILURE') {
                    script{
                        v1_CommonMethods_cenm.excludeCIDR()
                    }
                }
            }
        }
        stage('CCD level version check'){
            steps {
                catchError(stageResult: 'FAILURE') {
                    withCredentials([sshUserPrivateKey(credentialsId: 'ccd_client_master_key', keyFileVariable: 'ccd_client_master_key')]){
                        script{
                            v1_CommonMethods_cenm.ccdLvlVersionCheck()
                        }
                    }
                }
            }
        }
        stage('cENM namespace creation'){
            steps {
                catchError(stageResult: 'FAILURE') {
                    script{
                        v1_CommonMethods_cenm.nameSpaceCreate()
                    }
                }
            }
        }
        stage('SNMP WA'){
            steps {
                catchError(stageResult: 'FAILURE') {
                    withCredentials([sshUserPrivateKey(credentialsId: 'ccd_client_master_key', keyFileVariable: 'ccd_client_master_key')]){
                        script{
                            v1_CommonMethods_cenm.snmpUI()
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        if("${Deployment}" != "Flexikube"){
                            archiveArtifacts artifacts: "sysconf_backups.tar.gz", allowEmptyArchive: true
                        }
                    }
                }
            }
        }
        stage('Site value file preparation') {
            when {
                environment name: 'Site_Value_Populate', value: 'Yes'
            }
            steps {
                script{
                    commonMethods.read_site_config_info_from_dit()
                    commonMethods.get_cn_build_version_info()
                    commonMethods.csar_pre_config_setup()
                    commonMethods.download_charts_release_area()
                    commonMethods.remove_tgz_with_artifacts()
                    commonMethods.remove_yaml_with_artifacts()
                    commonMethods.get_integration_charts_path()
                    commonMethods.updateIntegrationValues()
                    commonMethods.overriding_key_value_pairs()
                    sh("cat ${HOME_DIR}/cENM/Scripts/${integration_values_file_path}")
                    archiveArtifacts "cENM/Scripts/${integration_values_file_path}"
                }
            }
        }
    }
    post{
        failure {
            script{
                echo "Failure"
            }
        }
        aborted{
            script{
                echo "Aborted"
            }
        }
        success{
            script{
                echo "Success"
            }
        }
        always {
            script{
                currentBuild.displayName = "#${BUILD_NUMBER}-cENM_PreDeploymentTask: ${environment_name}"
            }
        }
    }
}
def clone_ci_repo(){
   sh '''
       [ -d eric-enm-integration-pipeline-code ] && rm -rf eric-enm-integration-pipeline-code
        git clone -b dkeys ${GERRIT_MIRROR}/OSS/com.ericsson.oss.containerisation/eric-enm-integration-pipeline-code
   '''
}
/*
def pullPatchset(){
    if (env.GERRIT_REFSPEC !='' && env.GERRIT_REFSPEC != "refs/heads/master") {
        sh '''
        pwd
        cd dets_automation_pipeline
        pwd
        git fetch ssh://gerrit.ericsson.se:29418/DETES/com.ericsson.de.stsoss/dets_automation_pipeline $GERRIT_REFSPEC && git checkout FETCH_HEAD
        '''
    }
}*/