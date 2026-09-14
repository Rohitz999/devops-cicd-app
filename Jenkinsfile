pipeline {
    agent any

    tools {
        jdk 'temurin-17'
        maven 'maven3'
    }

    environment {
        // --- App identity ---
        APP_NAME      = "devops-mega-app"
        RELEASE       = "1.0.0"

        // --- DockerHub ---
        DOCKER_HUB    = "rohitdockerhub01"
        IMAGE_NAME    = "${DOCKER_HUB}/${APP_NAME}"
        IMAGE_TAG     = "${RELEASE}-${BUILD_NUMBER}"

        // --- Integrations ---
        GITOPS_REPO   = "https://github.com/Rohitz999/devops-mega-gitops.git"
        JENKINS_URL   = "https://jenkins.mechnomax.co.in"

        // --- SonarQube ---
        SONAR_TOKEN   = credentials('jenkins-sonarqube-token')

        // --- Email Notification ---
        EMAIL_TO      = "rohitvishwakarma8082@gmail.com"
        EMAIL_FROM    = "rohitvishwakarma8082@gmail.com"
    }

    parameters {
        string(
            name: 'OVERRIDE_TAG',
            defaultValue: '',
            description: 'Optional: manual tag override (leave empty for auto)'
        )
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
                sh """
                    echo "===== Building Docker Image ====="
                    echo "Image: ${IMAGE_NAME}"
                    echo "Tag:   ${IMAGE_TAG}"

                    docker build \
                      -t ${IMAGE_NAME}:${IMAGE_TAG} \
                      -t ${IMAGE_NAME}:latest .
                """
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
                          ${IMAGE_NAME}:${IMAGE_TAG} \
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

                        echo "===== Pushing: ${IMAGE_TAG} ====="
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}

                        echo "===== Pushing: latest ====="
                        docker push ${IMAGE_NAME}:latest
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
                    docker rmi ${IMAGE_NAME}:${IMAGE_TAG} 2>/dev/null || true
                    docker rmi ${IMAGE_NAME}:latest 2>/dev/null || true
                    echo "✅ Cleanup completed"
                """
            }
            cleanWs()
        }

        success {
            echo "✅ Pipeline succeeded: ${IMAGE_NAME}:${IMAGE_TAG}"

            // ---- Email Notification (SUCCESS) ----
            emailext(
                subject: "✅ [SUCCESS] ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <html>
                    <body style="font-family: Arial, sans-serif;">
                      <h2 style="color: #00c853;">✅ Build Succeeded</h2>
                      <table style="border-collapse: collapse; padding: 8px;">
                        <tr><td style="padding: 6px;"><b>Job</b></td><td style="padding: 6px;">${env.JOB_NAME}</td></tr>
                        <tr><td style="padding: 6px;"><b>Build</b></td><td style="padding: 6px;">#${env.BUILD_NUMBER}</td></tr>
                        <tr><td style="padding: 6px;"><b>Image</b></td><td style="padding: 6px;">${IMAGE_NAME}:${IMAGE_TAG}</td></tr>
                        <tr><td style="padding: 6px;"><b>Duration</b></td><td style="padding: 6px;">${currentBuild.durationString}</td></tr>
                      </table>
                      <p>
                        <a href="${env.BUILD_URL}" style="background: #00d4ff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Build</a>
                        <a href="https://hub.docker.com/r/rohitdockerhub01/devops-mega-app/tags" style="background: #7b2ff7; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">DockerHub</a>
                        <a href="https://app.mechnomax.co.in" style="background: #00c853; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Live App</a>
                      </p>
                    </body>
                    </html>
                """,
                to: "${EMAIL_TO}",
                from: "${EMAIL_FROM}",
                mimeType: 'text/html'
            )
        }

        failure {
            echo "❌ Pipeline failed: ${IMAGE_NAME}:${IMAGE_TAG}"

            // ---- Email Notification (FAILURE) ----
            emailext(
                subject: "❌ [FAILED] ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <html>
                    <body style="font-family: Arial, sans-serif;">
                      <h2 style="color: #d50000;">❌ Build Failed</h2>
                      <table style="border-collapse: collapse; padding: 8px;">
                        <tr><td style="padding: 6px;"><b>Job</b></td><td style="padding: 6px;">${env.JOB_NAME}</td></tr>
                        <tr><td style="padding: 6px;"><b>Build</b></td><td style="padding: 6px;">#${env.BUILD_NUMBER}</td></tr>
                        <tr><td style="padding: 6px;"><b>Image</b></td><td style="padding: 6px;">${IMAGE_NAME}:${IMAGE_TAG}</td></tr>
                        <tr><td style="padding: 6px;"><b>Failed Stage</b></td><td style="padding: 6px;">${env.STAGE_NAME ?: 'Unknown'}</td></tr>
                      </table>
                      <p>
                        <a href="${env.BUILD_URL}console" style="background: #d50000; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">View Console Output</a>
                      </p>
                    </body>
                    </html>
                """,
                to: "${EMAIL_TO}",
                from: "${EMAIL_FROM}",
                mimeType: 'text/html'
            )
        }
    }
}
