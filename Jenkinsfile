pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Anji-44/student-management-devops.git'
            }
        }

        stage('Build and Test') {
            steps {
                sh './mvnw clean test'
            }
        }

        stage('Archive JAR') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar',
                    fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build -t student-management:${BUILD_NUMBER} .
                    docker tag student-management:${BUILD_NUMBER} student-management:latest
                '''
            }
        }

        stage('Docker Deploy') {
            steps {
                sh '''
                    docker stop student-management-container || true
                    docker rm student-management-container || true

                    docker run -d \
                        --name student-management-container \
                        -p 8083:8082 \
                        student-management:${BUILD_NUMBER}
                '''
            }
        }
    }
}
