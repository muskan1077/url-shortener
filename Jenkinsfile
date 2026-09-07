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
    }
}