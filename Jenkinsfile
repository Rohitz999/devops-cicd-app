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
                sh """
                    trivy image --exit-code 0 --severity HIGH,CRITICAL \
                    ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG} || true
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                sh """
                    echo \$DOCKER_CREDS_PSW | docker login -u \$DOCKER_CREDS_USR --password-stdin
                    docker push ${DOCKER_HUB}/${APP_NAME}:${IMAGE_TAG}
                """
            }
        }

        stage('Trigger GitOps Update') {
            steps {
                sh """
                    curl -X POST "http://jenkins.local/job/gitops-devops-mega-project/build?token=gitops-token"
                """
            }
        }
    }

    post {
        always {
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
