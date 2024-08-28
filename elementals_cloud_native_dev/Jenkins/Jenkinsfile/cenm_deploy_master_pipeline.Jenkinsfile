#!/usr/bin/env groovy

/* IMPORTANT:
 *
 * In order to make this pipeline work, the following configuration on Jenkins is required:
 * - slave with a specific label (see pipeline.agent.label below)
 * - Credentials Plugin should be installed and have the secrets with the following names:
 *   + c12a011-config-file (admin.config to access c12a011 cluster)
 */

//def bob = "bob/bob -r \${WORKSPACE}/jenkins/rulesets/ruleset2.0.yaml"

pipeline {
    agent {
        label env.SLAVE_LABEL
    }
    parameters {
        string(
            name: 'DEPLOYMENT_NAME',
            defaultValue: 'stsvp2eic07',
            description: 'Deployment name - must match with the name created in bucket \"eiap\" in MiniIO'
        )
        string(
            name: 'DEPLOYMENT_TYPE',
            defaultValue: 'upgrade',
            description: 'Deployment Type, set \"install\" or \"upgrade\"'
        )
        string(name: 'VERSION',
            defaultValue: '24.01.112',
            description: 'cENM product version'
        )
        string(name: 'METHOD',
            defaultValue: 'CHART',
            description: 'CHART or CSAR'
        )
        string(name: 'SLAVE_LABEL',
            defaultValue: 'cENM',
            description: 'Kubernetes configuration file to specify which environment to install on'
        )
    }
    environment {
        USE_TAGS = 'true'
        STATE_VALUES_FILE = "site_values_${params.INT_CHART_VERSION}.yaml"
        PATH_TO_HELMFILE = "${params.INT_CHART_NAME}/helmfile.yaml"
    }
    stages {
        stage('Set build name') {
            steps {
                script {
                    currentBuild.displayName = "${env.BUILD_NUMBER} - ${params.DEPLOYMENT_TYPE}"
                }
            }
        }
        stage('PREINSTALL') {
            steps{
                echo "Running preinstall job"
                build job: 'Pre_deploy',
                parameters: [string(name: 'DEPLOYMENT_NAME', value: "${params.DEPLOYMENT_NAME}"),string(name: 'DEPLOYMENT_TYPE', value: "${params.DEPLOYMENT_TYPE}")]
            }
        }
        stage ('cENM Deploy'){
            steps {
                echo "Running install job"
                build job: 'cenm_deploy',
                parameters: [string(name: 'INT_CHART_VERSION', value: "${params.INT_CHART_VERSION}"),
                string(name: 'DEPLOYMENT_NAME', value: "${params.DEPLOYMENT_NAME}")]
            }
        }
        stage('Post Deploy'){
            steps{
                echo "Post Deploy steps"
                build job: 'Post_deploy'
            }
        }
    }
    post {
        always{
            //cleanWs disableDeferredWipeout: true
            sh "echo 'skip post'"
        }
    }

}


