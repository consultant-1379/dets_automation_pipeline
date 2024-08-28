pipeline {
    agent {
        label env.SLAVE_LABEL
    }
    parameters {
        string(
            name: 'SLAVE_LABEL',
            defaultValue: 'cENM',
            description: 'Specify the slave label that you want the job to run on'
        )
        string(
            name: 'ENV_NAME',
            defaultValue: 'stsvp6aenm01',
            description: 'The name of the environment (i.e stsvp6aenm01) '
        )
        
        booleanParam( 
            defaultValue: true, 
            description: 'Delete old backups after validation', 
            name: 'DELETE_OLD_BACKUPS'
        )
        string(
            name: 'EMAIL_LIST', 
            defaultValue: 'adam.gajak.ext@ericsson.com,lukasz.matysiak.ext@ericsson.com,krzysztof.blaszczyk.ext@ericsson.com', 
            description: 'List of emails which will be notified about this job failed'
        )
    }
    stages {
        stage('Set build name') {
            steps {
                script {
                    env.currentDate = sh(returnStdout: true, script: 'date +%d-%m-%Y').trim()
                    currentBuild.displayName = "${env.BUILD_NUMBER} - ${params.ENV_NAME} - ${env.currentDate} "                 
                }
            }
        }

        stage('Get VNFLCM IP') {
            steps {
                echo "Getting VNLCM IP"
                get_VNFLCM_IP()
            }
        }

        stage('Create Backup') {
            steps {
                echo "Creating Backup"
                sh "vENM/scripts/create_backup.sh ${env.VNFLCM_IP} ${params.ENV_NAME} ${env.BUILD_NUMBER} ${env.currentDate}"
                
            }
        }
        stage('Validate Backup') {
            steps {
                echo "Validating the backup"
                sh "vENM/scripts/validate_backup.sh ${env.VNFLCM_IP} ${params.ENV_NAME} ${env.BUILD_NUMBER} ${env.currentDate}"
            }
        }
        stage('Delete old backups') {
            when {
                expression {"${params.DELETE_OLD_BACKUPS}" == 'true' }
            }
            steps {
               echo "Deleting old backups"
               sh"vENM/scripts/delete_old_backups.sh ${env.VNFLCM_IP} ${params.ENV_NAME} ${env.BUILD_NUMBER} ${env.currentDate}"
            }
        }
    }
    post {
        always {
            cleanWs disableDeferredWipeout: true
        }
        failure{
                    mail to: "${params.EMAIL_LIST}",
                        subject: "vENM backup failed - ${params.ENV_NAME}_backup_${env.BUILD_NUMBER}_${env.currentDate}",
                        body: "Link to VNFLCM:\n https://${env.VNFLCM_IP}/index.html#workflows  \n\nLink to jenkins  job:\n ${BUILD_URL} "
        }
        success{
                    mail to: "${params.EMAIL_LIST}",
                        subject: "vENM backup SUCCESS - ${params.ENV_NAME}_backup_${env.BUILD_NUMBER}_${env.currentDate}",
                        body: "Link to VNFLCM:\n https://${env.VNFLCM_IP}/index.html#workflows  \n\nLink to jenkins  job:\n ${BUILD_URL} "
        }
    }
}

def get_VNFLCM_IP(){
    
    env.VNFLCM_IP=sh(returnStdout: true, script: """ curl -s "https://atvdit.athtem.eei.ericsson.se/api/documents?q=name=VNFLCM_${params.ENV_NAME}&fields=content(parameters(external_ipv4_for_services_vm))" | jq -r  .[].content.parameters.external_ipv4_for_services_vm""" ).trim()
}