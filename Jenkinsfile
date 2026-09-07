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
                sh '/usr/local/bin/docker build -t url-shortener:1.0 .'
            }
        }
    }
}