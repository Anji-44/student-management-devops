// pipeline verification update

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
            description: 'Docker image version to deploy during rollback, e.g. 44'
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

                        PREVIOUS_IMAGE=$(docker images student-management \
                            --format "{{.Repository}}:{{.Tag}}" | \
                            grep -E 'student-management:[0-9]+$' | \
                            grep -v "student-management:${BUILD_NUMBER}" | \
                            sort -t: -k2,2nr | \
                            head -n 1)

                        if [ -n "$PREVIOUS_IMAGE" ]; then
                            echo "$PREVIOUS_IMAGE" > previous-version.txt
                            echo "Previous version saved: $PREVIOUS_IMAGE"
                        else
                            echo "No previous version available."
                            rm -f previous-version.txt
                        fi

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

                    i=1

                    while [ "$i" -le 10 ]; do

                        if curl -f http://localhost:8083/students; then
                            echo ""
                            echo "Application is responding!"
                            break
                        fi

                        echo "Application not ready yet... retrying in 3 seconds"
                        sleep 3

                        i=$((i + 1))
                    done

                    if [ "$i" -gt 10 ]; then
                        echo "ERROR: Application did not start."
                        exit 1
                    fi

                    echo "Checking Docker container health..."

                    i=1

                    while [ "$i" -le 10 ]; do

                        STATUS=$(docker inspect \
                            --format='{{.State.Health.Status}}' \
                            student-management-container)

                        echo "Health status: $STATUS"

                        if [ "$STATUS" = "healthy" ]; then
                            echo "Docker container is healthy!"
                            exit 0
                        fi

                        sleep 3

                        i=$((i + 1))
                    done

                    echo "ERROR: Docker container did not become healthy."
                    exit 1
                '''
            }
        }

        stage('Docker Cleanup') {
            steps {
                sh '''
                    echo "Docker Cleanup started..."

                    IMAGES=$(docker images student-management \
                        --format "{{.Tag}}" | \
                        grep -E '^[0-9]+$' | \
                        sort -nr)

                    KEEP=$(echo "$IMAGES" | head -n 3)

                    for IMAGE in $IMAGES; do

                        if ! echo "$KEEP" | grep -qx "$IMAGE"; then

                            echo "Removing old image: student-management:$IMAGE"

                            docker rmi -f \
                                "student-management:$IMAGE" || true

                        fi

                    done

                    echo "Keeping versions:"
                    echo "$KEEP"

                    echo "Docker Cleanup completed!"
                '''
            }
        }
    }

    post {
        failure {
            sh '''
                if [ "$DEPLOY_MODE" = "NORMAL" ] && [ -f previous-version.txt ]; then

                    PREVIOUS_IMAGE=$(cat previous-version.txt)

                    if [ -n "$PREVIOUS_IMAGE" ]; then

                        echo "Deployment failed!"
                        echo "Automatically rolling back to $PREVIOUS_IMAGE"

                        docker stop student-management-container || true
                        docker rm student-management-container || true

                        docker run -d \
                            --name student-management-container \
                            --restart unless-stopped \
                            -p 8083:8082 \
                            "$PREVIOUS_IMAGE"

                        echo "Automatic rollback completed: $PREVIOUS_IMAGE"

                    fi

                else
                    echo "No automatic rollback required."
                fi
            '''
        }
    }
}
