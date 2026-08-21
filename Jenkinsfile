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

                if ! docker image inspect "$IMAGE" > /dev/null 2>&1; then
                    echo "ERROR: Docker image $IMAGE does not exist."
                    exit 1
                fi

                docker stop student-management-container 2>/dev/null || true
                docker rm student-management-container 2>/dev/null || true

                docker run -d \
                    --name student-management-container \
                    --restart unless-stopped \
                    -p 8083:8082 \
                    "$IMAGE"

                echo "Rollback completed successfully to version ${ROLLBACK_VERSION}"

            else

                IMAGE="student-management:${BUILD_NUMBER}"

                echo "Deploying new version ${IMAGE}"

                PREVIOUS_IMAGE=$(docker images student-management \
                    --format '{{.Repository}}:{{.Tag}}' \
                    | grep -v "student-management:${BUILD_NUMBER}" \
                    | sort -k2,2nr \
                    | grep -E 'student-management:[0-9]+$' \
                    | head -n 1 || true)

                if [ -n "$PREVIOUS_IMAGE" ]; then
                    echo "$PREVIOUS_IMAGE"
                    echo "Previous version saved: $PREVIOUS_IMAGE"
                fi

                docker stop student-management-container 2>/dev/null || true
                docker rm student-management-container 2>/dev/null || true

                docker run -d \
                    --name student-management-container \
                    --restart unless-stopped \
                    -p 8083:8082 \
                    "$IMAGE"

                echo "$IMAGE"

            fi
        '''
            }
        }

        stage('Verify Deployment') {
    steps {
        sh '''
            echo "Waiting for application to start..."

            i=1
            while [ $i -le 10 ]; do
                if curl -f http://localhost:8083/students; then
                    echo ""
                    echo "Application is responding!"
                    break
                fi

                echo "Application not ready yet... retrying in 3 seconds"
                sleep 3
                i=$((i + 1))
            done

            if [ $i -gt 10 ]; then
                echo "ERROR: Application failed to start."
                exit 1
            fi

            echo "Checking Docker container health..."

            i=1
            while [ $i -le 10 ]; do
                STATUS=$(docker inspect --format='{{.State.Health.Status}}' student-management-container 2>/dev/null || echo "unknown")

                echo "Health status: $STATUS"

                if [ "$STATUS" = "healthy" ]; then
                    echo "Docker container is healthy!"
                    break
                fi

                echo "Container not healthy yet... retrying in 3 seconds"
                sleep 3
                i=$((i + 1))
            done

            if [ $i -gt 10 ]; then
                echo "ERROR: Docker container did not become healthy."
                exit 1
            fi

            if [ "$DEPLOY_MODE" = "ROLLBACK" ]; then
                EXPECTED_IMAGE="student-management:${ROLLBACK_VERSION}"
                ACTUAL_IMAGE=$(docker inspect --format='{{.Config.Image}}' student-management-container)

                echo "Expected rollback image: $EXPECTED_IMAGE"
                echo "Actual running image: $ACTUAL_IMAGE"

                if [ "$ACTUAL_IMAGE" != "$EXPECTED_IMAGE" ]; then
                    echo "ERROR: Rollback verification failed!"
                    exit 1
                fi

                echo "Rollback verification successful!"
                echo "Running version: ${ROLLBACK_VERSION}"
            else
                EXPECTED_IMAGE="student-management:${BUILD_NUMBER}"
                ACTUAL_IMAGE=$(docker inspect --format='{{.Config.Image}}' student-management-container)

                echo "Expected deployment image: $EXPECTED_IMAGE"
                echo "Actual running image: $ACTUAL_IMAGE"

                if [ "$ACTUAL_IMAGE" != "$EXPECTED_IMAGE" ]; then
                    echo "ERROR: Deployment verification failed!"
                    exit 1
                fi

                echo "Deployment verification successful!"
                echo "Running version: ${BUILD_NUMBER}"
            fi

            exit 0
        '''
            }
       }

        stage('Docker Cleanup') {
            steps {
                sh '''
                    docker image prune -f
                    echo "Docker cleanup completed!"
                '''
           }
        } 
    }
}
