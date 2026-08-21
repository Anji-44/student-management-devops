# Student Management System - CI/CD DevOps Project

## Project Overview

A Spring Boot Student Management application deployed using a complete CI/CD pipeline with GitHub, Jenkins, Maven, Docker, automated health checks, Docker image versioning, cleanup, and rollback.

## Technologies

- Java 21
- Spring Boot
- Maven
- JUnit
- Git & GitHub
- Jenkins
- Docker
- AWS EC2
- Linux

## CI/CD Pipeline

1. Checkout
2. Build and Test
3. Package
4. Archive JAR
5. Docker Build
6. Docker Deploy
7. Verify Deployment
8. Docker Cleanup

## Docker Features

- Versioned Docker images using Jenkins BUILD_NUMBER
- latest Docker tag
- Automatic container deployment
- unless-stopped restart policy
- Docker health check
- Automatic cleanup of old images
- Automatic rollback to a previous version
- Rollback version verification

## Deployment

Jenkins automatically builds and deploys the application whenever changes are pushed to GitHub.

Application port:

8083 → 8082

## Rollback

The pipeline supports rollback using:

- DEPLOY_MODE
- ROLLBACK_VERSION

The pipeline verifies that the requested rollback image is actually running.

## Verification

The pipeline verifies:

- Application availability
- Docker container health
- Expected Docker image version
- Rollback image version

## Result

The Student Management application is automatically built, tested, containerized, deployed, verified, cleaned up, and rolled back when required.
