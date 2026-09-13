pipeline {
    agent any

    tools {
        jdk 'temurin-17'
        maven 'maven3'
    }

    environment {
        APP_NAME      = "devops-mega-app"
        DOCKER_HUB    = "rohitdockerhub01"
        IMAGE_TAG     = "${params.IMAGE_TAG}"
        SONAR_TOKEN   = credentials('jenkins-sonarqube-token')
        GITOPS_REPO   = "https://github.com/YOUR-GITHUB-USERNAME/devops-mega-gitops.git"
    }

    parameters {
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Cloning app repo..."
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('Sonarqube-scanner') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG} ."
            }
        }

        stage('Trivy Scan') {
            steps {
                script {
                    sh """
                        mkdir -p reports

                        docker run --rm \
                          --name trivy-scan \
                          -v /var/run/docker.sock:/var/run/docker.sock \
                          -v /var/lib/jenkins/.trivy-cache:/root/.cache/trivy \
                          -v \$(pwd)/reports:/reports \
                          aquasec/trivy:latest image \
                          ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG} \
                          --no-progress \
                          --scanners vuln \
                          --exit-code 0 \
                          --severity HIGH,CRITICAL \
                          --format table \
                          --output /reports/trivy-report.txt

                        echo "===== Trivy Scan Report ====="
                        cat reports/trivy-report.txt || echo "No report file"
                        echo "============================="

                        docker rm -f trivy-scan 2>/dev/null || true
                    """
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'reports/*', allowEmptyArchive: true
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                        docker push ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Trigger GitOps Update') {
            steps {
                sh """
                    curl -X POST "http://jenkins.local/job/gitops-devops-mega-project/build?token=gitops-token" || true
                """
            }
        }

        stage('Cleanup Artifacts') {
            steps {
                script {
                    sh """
                        docker rmi ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG} 2>/dev/null || true
                        docker rmi ${DOCKER_HUB}/${APP_NAME}:latest 2>/dev/null || true
                        docker rm -f trivy-scan trivy-scan-table 2>/dev/null || true
                        docker image prune -f 2>/dev/null || true
                        echo "✅ Cleanup completed"
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                sh """
                    docker rm -f trivy-scan trivy-scan-table 2>/dev/null || true
                """
            }
            cleanWs()
        }
        success {
            echo "✅ Pipeline succeeded for ${APP_NAME}:${IMAGE_TAG}"
        }
        failure {
            echo "❌ Pipeline failed"
        }
    }
}
