
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_HOST = '172.31.29.131'
        APP_USER = 'ubuntu'
        APP_DIR = '/opt/securebank'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Deploy to SecureBank EC2') {
            steps {
                sshagent(credentials: ['securebank-ec2-ssh']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=accept-new \
                            ${APP_USER}@${APP_HOST} \
                            "cd ${APP_DIR} && \
                             git pull origin main && \
                             docker compose config -q && \
                             docker compose up -d --build"
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                sshagent(credentials: ['securebank-ec2-ssh']) {
                    sh '''
                        ssh ${APP_USER}@${APP_HOST} \
                            "cd ${APP_DIR} && \
                             docker compose ps"
                    '''
                }
            }
        }
    }
}
