//def commonMethods
def CommonMethods_cenm

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
        /*stage('Checkout Integration Pipeline Git Repository') {
            steps {
                clone_Integration_repo()
                clone_dets_repo()
                pullPatchset()
            }
        }*/
        stage('Checkout dets_automation Pipeline Git Repository') {
            steps {
                git branch: 'master',
                        url: 'ssh://gerrit.ericsson.se:29418/DETES/com.ericsson.de.stsoss/dets_automation_pipeline'
                sh '''
                    git remote set-url origin --push ssh://gerrit.ericsson.se:29418/DETES/com.ericsson.de.stsoss/dets_automation_pipeline
                '''
                clone_Integration_repo()
                //pullPatchset()
            }
        }
        stage('Load common methods') {
            steps {
                script {
                    //commonMethods = load("${env.WORKSPACE}/eric-enm-integration-pipeline-code/Jenkins/JobDSL/CommonMethods.groovy")
                    v1_CommonMethods_cenm = load("${env.WORKSPACE}/v1_cENM_pipeline/JobDSL/v1_CommonMethods_cenm.groovy")
                }
            }
        }
        /*stage('Delete docker image') {
            steps {
                script {
                    CommonMethods_cenm.delete_build_utilities_image()
                }
            }
        }
        stage('Pull docker image') {
            steps {
                script {
                    sh "docker pull ${cenm_utilities_docker_image}"
                }
            }
        }*/
        stage('PreConfig') {
            steps {
                withCredentials([
                sshUserPrivateKey(credentialsId: 'ccd_client_master_key', keyFileVariable: 'ccd_client_master_key'),
                usernamePassword(credentialsId: 'detsFunUser', passwordVariable: 'detspassword', usernameVariable: 'detsusername')
                ]) {
                    configFileProvider([configFile(fileId: "burInventory.csv", targetLocation: "${env.WORKSPACE}")]) {}
                    configFileProvider([configFile(fileId: "inventory.csv", targetLocation: "${env.WORKSPACE}")]) {}
                    script {
                        v1_CommonMethods_cenm.extract_jq()
                        try{
                            v1_CommonMethods_cenm.check_clusterID()
                        }catch(error){
                            echo "${error}"
                            throw error
                        }
                        v1_CommonMethods_cenm.domainIPGet()
                        v1_CommonMethods_cenm.set_site_info_env()
                    }
                }
            }
        }
        stage('Load and Set Kubeconfig from Creds') {
            when {
                expression { params.KUBECONFIG_CREDENTIALS != '' }
            }
            steps {
                catchError(stageResult: 'FAILURE') {
                    withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS, variable: 'KUBECONFIG')]) {
                        script {
                            v1_CommonMethods_cenm.load_kubeconfig_from_credentials()
                        }
                    }
                }
            }
        }
        stage('DIT Kubeconfig Check') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'ccd_client_master_key', keyFileVariable: 'ccd_client_master_key'),
                    usernamePassword(credentialsId: 'detsFunUser', passwordVariable: 'detspassword', usernameVariable: 'detsusername')
                    ]) {
                    script {
                        v1_CommonMethods_cenm.kube_config_file_integrity_check()
                        v1_CommonMethods_cenm.upload_kube_to_dit()
                    }
                }
            }
        }
        stage('Set DIT Kubeconfig') {
            when {
                environment name: 'KUBECONFIG_FROM_CREDENTIALS', value: 'No'
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'ccd_client_master_key', keyFileVariable: 'ccd_client_master_key'),
                    usernamePassword(credentialsId: 'detsFunUser', passwordVariable: 'detspassword', usernameVariable: 'detsusername')
                    ]) {
                    script{
                        v1_CommonMethods_cenm.download_kube_config_file_from_dit()
                        v1_CommonMethods_cenm.set_kube_config_file()
                        //CommonMethods_cenm.brocli_configuration()
                    }
                }
            }
        }
        stage('SetEnv - K8s') {
            steps {
                script {
                    v1_CommonMethods_cenm.set_k8s_env()
                }
            }
        }
        stage('Ingress Certificate tls enm ui ca'){
            when{
                environment name: 'tls_cert_task', value: 'Yes'
            }
            steps {
                script{
                    v1_CommonMethods_cenm.ingressCert()
                }
            }
        }
        stage('Update config map with ENM version'){
            when{
                environment name: 'install_type', value: 'chart'
            }
            steps {
                echo "Preparing configmap"
                sh"""
                product_drop=\$(echo $product_set_version | cut -d '.' -f 1,2)
                echo \$product_drop
                csar_version=\$(curl -4 --location --silent --request  GET https://ci-portal.seli.wh.rnd.internal.ericsson.com/api/cloudnative/getCloudNativeProductSetContent/\$product_drop/$product_set_version/ | jq -r '.[0].csar_data[] | select(.csar_name == "enm-installation-package")'.csar_version)
                kubectl --kubeconfig=$kubeConfig get cm eric-enm-version-configmap -n $NAMESPACE -o yaml > version_cm
                sed -i '/creationTimestamp/d' version_cm
                sed -i '/resourceVersion/d' version_cm
                sed -i "s/SPRINT_TAG/\$product_drop/g" version_cm
                sed -i "s/CSAR_VERSION/\$csar_version/g" version_cm
                sed -i 's/AOM_RSTATE/$RSTATE/g' version_cm
                cat version_cm
                kubectl --kubeconfig=$kubeConfig apply -f version_cm
                kubectl --kubeconfig=$kubeConfig rollout restart deployment uiserv -n $NAMESPACE
                """
            }
        }
        stage('Backup Configuration'){
            when {
                environment name: 'backup_configuration', value: 'Yes'
            }
            stages {
                stage('Is cENM alive'){
                    steps {
                        script{
                            v1_CommonMethods_cenm.smokeTest()
                        }
                    }
                }
                stage('Backup configuration pre-requsite'){
                    steps {
                        script{
                            v1_CommonMethods_cenm.backupHealthCheck()
                            v1_CommonMethods_cenm.domainCheck()
                            v1_CommonMethods_cenm.backupConfigurePreTask()
                        }
                    }
                }
                stage('Backup configuration'){
                    steps {
                        script{
                            v1_CommonMethods_cenm.retenionCheck()
                            v1_CommonMethods_cenm.backupScheduling()
                            v1_CommonMethods_cenm.addscheduleInterval()
                        }
                    }
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
                currentBuild.displayName = "#${BUILD_NUMBER}-cENM_PostDeploymentTask: ${environment_name}"
            }
        }
    }
}

def clone_Integration_repo(){
   sh '''
       [ -d eric-enm-integration-pipeline-code ] && rm -rf eric-enm-integration-pipeline-code
        git clone ${GERRIT_MIRROR}/OSS/com.ericsson.oss.containerisation/eric-enm-integration-pipeline-code
   '''
}

/*def clone_dets_repo(){
   sh '''
       [ -d dets_automation_pipeline ] && rm -rf dets_automation_pipeline
        git clone ${GERRIT_CENTRAL}/DETES/com.ericsson.de.stsoss/dets_automation_pipeline
   '''
}

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