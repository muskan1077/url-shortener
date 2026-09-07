pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '/opt/homebrew/bin/mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh '/opt/homebrew/bin/mvn test'
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    export PATH="/usr/local/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"
                    which docker
                    which docker-credential-desktop
                    docker build -t url-shortener:1.0 .
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    export PATH="/usr/local/bin:/Applications/Docker.app/Contents/Resources/bin:$PATH"

                    docker compose down
                    docker compose up -d
                '''
            }
        }
    }
}