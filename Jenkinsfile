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
        DOCKER_CREDS  = credentials('dockerhub-creds')
        GITOPS_REPO   = "https://github.com/your-username/devops-mega-gitops.git"
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
                          -v /var/lib/jenkins/.trivy-tmp:/tmp \
                          -v \$(pwd)/reports:/reports \
                          aquasec/trivy:latest image \
                          ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG} \
                          --no-progress \
                          --scanners vuln \
                          --skip-java-db-update \
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
                withCredentials([usernamePassword(
                    credentialsId: 'jenkins-api-token',
                    usernameVariable: 'JENKINS_USER',
                    passwordVariable: 'JENKINS_API_TOKEN'
                )]) {
                    sh """
                        echo "===== Triggering GitOps Pipeline ====="
                        echo "Target: ${JENKINS_URL}/job/devops-mega-gitops"
                        echo "IMAGE_TAG: ${IMAGE_TAG}"

                        HTTP_CODE=\$(curl -s -o /dev/null -w "%{http_code}" \
                          --user "\$JENKINS_USER:\$JENKINS_API_TOKEN" \
                          -X POST \
                          -H 'cache-control: no-cache' \
                          -H 'content-type: application/x-www-form-urlencoded' \
                          --data "IMAGE_TAG=${IMAGE_TAG}" \
                          "${JENKINS_URL}/job/devops-mega-gitops/buildWithParameters?token=gitops-token")

                        echo "HTTP response code: \$HTTP_CODE"

                        if [ "\$HTTP_CODE" = "201" ] || [ "\$HTTP_CODE" = "200" ]; then
                            echo "✅ GitOps pipeline triggered successfully"
                        else
                            echo "❌ GitOps trigger failed with HTTP \$HTTP_CODE"
                            exit 1
                        fi

                        echo "===== Trigger Completed ====="
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
                    docker rmi ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG} 2>/dev/null || true
                    docker rmi ${DOCKER_HUB}/${APP_NAME}:latest 2>/dev/null || true
                    echo "✅ Cleanup completed"
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
