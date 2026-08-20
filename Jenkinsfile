pipeline {
    agent any

    parameters {
        choice(
            name: 'DEPLOY_MODE',
            choices: ['NORMAL', 'ROLLBACK'],
            description: 'Choose NORMAL for a new deployment or ROLLBACK for a previous Docker image.'
        )
        string(
            name: 'ROLLBACK_VERSION',
            defaultValue: '',
            description: 'Docker image version to deploy during rollback, e.g. 20'
        )
    }

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

        stage('Package') {
            steps {
                sh './mvnw package -DskipTests'
            }
        }

        stage('Archive JAR') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar',
                    fingerprint: true
            }
        }

        stage('Docker Build') {
            when {
                expression {
                    params.DEPLOY_MODE == 'NORMAL'
                }
            }
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
            if [ "$DEPLOY_MODE" = "ROLLBACK" ]; then
                if [ -z "$ROLLBACK_VERSION" ]; then
                    echo "ERROR: ROLLBACK_VERSION is required for rollback."
                    exit 1
                fi

                IMAGE="student-management:${ROLLBACK_VERSION}"
                echo "Rolling back to ${IMAGE}"
            else
                IMAGE="student-management:${BUILD_NUMBER}"
                echo "Deploying new version ${IMAGE}"
            fi

            docker stop student-management-container || true
            docker rm student-management-container || true

            docker run -d \
                --name student-management-container \
                --restart unless-stopped \
                -p 8083:8082 \
                "$IMAGE"

            echo "$IMAGE" > deployed-version.txt
        '''
            }
        }

        stage('Verify Deployment') {
    steps {
        sh '''
            echo "Waiting for application to start..."
            sleep 10

            echo "Verifying application deployment..."
            RESPONSE=$(curl -s --fail http://localhost:8083/students)

            echo "Application response:"
            echo "$RESPONSE"

            echo "$RESPONSE" | grep -q "Student Management System"

            echo "Deployment verification successful!"
        '''
            }
       }

        stage('Docker Cleanup') {
    steps {
        sh '''
            docker images student-management --format "{{.Repository}}:{{.Tag}}" | \
            grep -E 'student-management:[0-9]+$' | \
            grep -v "student-management:${BUILD_NUMBER}" | \
            xargs -r docker rmi -f

            echo "Docker Cleanup completed!"
        '''
           }
        } 
    }
}
