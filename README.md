# 🚀 DevOps Mega App — End-to-End CI/CD Pipeline

[![Jenkins](https://img.shields.io/badge/Jenkins-CI/CD-blue?logo=jenkins)](https://jenkins.mechnomax.co.in)
[![SonarQube](https://img.shields.io/badge/SonarQube-Quality%20Gate-4E9BCD?logo=sonarqube)](https://sonarqube.mechnomax.co.in)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker)](https://hub.docker.com/r/rohitdockerhub01/devops-mega-app)
[![Trivy](https://img.shields.io/badge/Trivy-Security%20Scan-1904DA)](https://trivy.dev)
[![ArgoCD](https://img.shields.io/badge/ArgoCD-GitOps-EF7B4D?logo=argo)](https://argocd.mechnomax.co.in)
[![K3s](https://img.shields.io/badge/K3s-Kubernetes-FFC61C?logo=k3s)](https://k3s.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Spring Boot application demonstrating a complete end-to-end DevOps workflow: from code push to live HTTPS deployment in ~2 minutes.**

🌐 **Live Demo:** [https://app.mechnomax.co.in](https://app.mechnomax.co.in)

---

## 📖 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Repositories](#repositories)
- [Pipeline Stages](#pipeline-stages)
- [Getting Started](#getting-started)
- [Application Endpoints](#application-endpoints)
- [Project Structure](#project-structure)
- [Pipeline Configuration](#pipeline-configuration)
- [Docker Image](#docker-image)
- [Day-to-Day Workflow](#day-to-day-workflow)
- [Troubleshooting](#troubleshooting)
- [Related Repositories](#related-repositories)
- [License](#license)

---

## Overview

This repository contains the **application code** for the DevOps Mega Project — a fully automated CI/CD pipeline that takes a `git push` all the way to a live HTTPS deployment on Kubernetes.

**What happens on every push:**

```
git push
   ↓
Jenkins builds + tests + scans
   ↓
Docker image → DockerHub (unique tag per build)
   ↓
Triggers GitOps pipeline
   ↓
ArgoCD syncs to K3s
   ↓
https://app.mechnomax.co.in updates live
```

**Key features:**
- ⚡ Fully automated — zero manual steps after push
- 🔒 Automatic SSL via Let's Encrypt
- 📊 Code quality gate enforced by SonarQube
- 🛡️ Container vulnerability scanning by Trivy
- 🐳 Multi-stage Docker builds with unique tags (`1.0.0-<build#>`)
- 🔄 Rolling updates with zero downtime
- 📧 Email notifications on success/failure

---

## Architecture

```
┌──────────────────────┐
│  Developer           │
│  (git push)          │
└──────────┬───────────┘
           │
           ▼
┌─────────────────────────────────────────────┐
│  GitHub — devops-cicd-app                   │
│  (this repo)                                │
└──────────┬──────────────────────────────────┘
           │ webhook / poll
           ▼
┌─────────────────────────────────────────────┐
│  Jenkins Job 1: devops-cicd-app             │
│  ──────────────────────────────────────     │
│  ✓ Checkout                                 │
│  ✓ Maven Build                              │
│  ✓ Unit Tests                               │
│  ✓ SonarQube Analysis + Quality Gate        │
│  ✓ Docker Build (tag: 1.0.0-<build#>)       │
│  ✓ Trivy Scan                               │
│  ✓ Push to DockerHub                        │
│  ✓ Trigger GitOps Job                       │
│  ✓ Email Notification                       │
└──────────┬──────────────────────────────────┘
           │
           ▼
┌──────────────────────┐
│  DockerHub           │
│  rohitdockerhub01/   │
│  devops-mega-app     │
└──────────┬───────────┘
           │ (via GitOps repo)
           ▼
┌──────────────────────┐
│  ArgoCD → K3s        │
│  https://app.        │
│  mechnomax.co.in     │
└──────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Language** | Java 17 | Application runtime |
| **Framework** | Spring Boot 3.2 | REST API + static frontend |
| **Build** | Maven | Dependency + build management |
| **Testing** | JUnit 5 | Unit tests |
| **CI** | Jenkins | Build orchestration |
| **Code Quality** | SonarQube | Static analysis + quality gate |
| **Container** | Docker | Multi-stage builds |
| **Security** | Trivy | Vulnerability scanning |
| **Registry** | DockerHub | Image storage |
| **CD** | ArgoCD | GitOps continuous delivery |
| **Orchestration** | K3s | Lightweight Kubernetes |
| **Ingress** | Nginx | HTTP routing |
| **SSL** | cert-manager | Automatic Let's Encrypt |

---

## Repositories

| Repo | Purpose |
|------|---------|
| **`devops-cicd-app`** (this) | Java app + Dockerfile + main Jenkinsfile |
| [`devops-mega-gitops`](https://github.com/Rohitz999/devops-mega-gitops) | K8s manifests + GitOps Jenkinsfile + ArgoCD config |

---

## Pipeline Stages

The `Jenkinsfile` defines the following stages:

| # | Stage | What it does |
|---|-------|--------------|
| 1 | **Checkout** | Clones this repo |
| 2 | **Build** | Runs `mvn clean package -DskipTests` |
| 3 | **Unit Tests** | Runs `mvn test` (3 JUnit tests) |
| 4 | **SonarQube Analysis** | Static code analysis via SonarQube |
| 5 | **Quality Gate** | Fails build if quality gate fails |
| 6 | **Build Docker Image** | Multi-stage build → `1.0.0-<build#>` + `latest` |
| 7 | **Trivy Scan** | Vulnerability scan (HIGH + CRITICAL) |
| 8 | **Push Docker Image** | Pushes both tags to DockerHub |
| 9 | **Trigger GitOps Update** | Triggers downstream GitOps pipeline |
| 10 | **Notification** | Sends email on success or failure |

**Image tags:** Every build gets a unique tag `1.0.0-<build#>` — this is what triggers ArgoCD to sync.

---

## Getting Started

### Prerequisites

- **Java 17** (Temurin)
- **Maven 3.9+**
- **Docker**
- **Jenkins** (for CI)

### Local Development

```bash
# Clone the repo
git clone https://github.com/Rohitz999/devops-cicd-app.git
cd devops-cicd-app

# Build the app
mvn clean package

# Run tests
mvn test

# Run locally
java -jar target/devops-mega-app-1.0.0.jar
```

Open http://localhost:8080 to see the app.

### Build with Docker

```bash
# Build the image
docker build -t devops-mega-app:local .

# Run
docker run -d --name devops-app -p 8080:8080 devops-mega-app:local

# Test
curl http://localhost:8080/health
curl http://localhost:8080/version
curl http://localhost:8080/api
```

### Run the Full Pipeline

Push to `main` → Jenkins auto-detects (if webhook configured) or manually trigger:

**Jenkins → `devops-cicd-app` → Build Now**

The pipeline:
1. Builds the app
2. Runs tests
3. Scans with SonarQube + Trivy
4. Builds and pushes Docker image `1.0.0-<build#>`
5. Triggers the GitOps pipeline
6. ArgoCD deploys to K3s
7. Live app updates

---

## Application Endpoints

| Endpoint | Method | Response | Purpose |
|----------|--------|----------|---------|
| `/` | GET | HTML page | Landing page with architecture + status |
| `/health` | GET | `OK` | Health check |
| `/version` | GET | `v1.0.0` | App version |
| `/api` | GET | `DevOps Mega Project - CI/CD Pipeline Working!` | Simple API |
| `/actuator/health` | GET | `{"status":"UP"}` | Spring Boot Actuator |
| `/actuator/info` | GET | App metadata | Actuator info |

**Live examples:**

```bash
curl https://app.mechnomax.co.in/health
# → OK

curl https://app.mechnomax.co.in/version
# → v1.0.0

curl https://app.mechnomax.co.in/api
# → DevOps Mega Project - CI/CD Pipeline Working!
```

---

## Project Structure

```
devops-cicd-app/
├── Dockerfile                          # Multi-stage build
├── Jenkinsfile                         # Main CI/CD pipeline
├── pom.xml                             # Maven config + plugins
├── README.md                           # This file
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   └── App.java                # Spring Boot entry + REST endpoints
    │   └── resources/
    │       ├── application.properties  # App config
    │       └── static/
    │           └── index.html          # Landing page (HTML + CSS + JS)
    └── test/
        └── java/com/example/
            └── AppTest.java            # JUnit 5 tests
```

---

## Pipeline Configuration

### Environment Variables (in `Jenkinsfile`)

| Variable | Value | Purpose |
|----------|-------|---------|
| `APP_NAME` | `devops-mega-app` | App identifier |
| `RELEASE` | `1.0.0` | Semantic version |
| `DOCKER_HUB` | `rohitdockerhub01` | DockerHub username |
| `IMAGE_NAME` | `${DOCKER_HUB}/${APP_NAME}` | Full image name |
| `IMAGE_TAG` | `${RELEASE}-${BUILD_NUMBER}` | Unique tag per build |
| `GITOPS_REPO` | `https://github.com/Rohitz999/devops-mega-gitops.git` | Downstream repo |
| `JENKINS_URL` | `https://jenkins.mechnomax.co.in` | Jenkins base URL |

### Jenkins Credentials Required

| Credential ID | Kind | Purpose |
|---------------|------|---------|
| `github-creds` | Username + Password | GitHub PAT for clone |
| `dockerhub-creds` | Username + Password | DockerHub token for push |
| `jenkins-sonarqube-token` | Secret text | SonarQube analysis token |
| `jenkins-api-token` | Username + Password | Trigger downstream GitOps job |
| `argocd-token` | Secret text | ArgoCD API (used by GitOps job) |

---

## Docker Image

**Multi-stage Dockerfile:**

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Advantages:**
- Final image has only JRE + JAR — no Maven
- Image size: ~180 MB
- Fast startup (~8 sec)

**Pull the image:**

```bash
docker pull rohitdockerhub01/devops-mega-app:latest
docker run -d -p 8080:8080 rohitdockerhub01/devops-mega-app:latest
```

**View all tags:** [DockerHub tags page](https://hub.docker.com/r/rohitdockerhub01/devops-mega-app/tags)

---

## Day-to-Day Workflow

### Make a Change

```bash
# Clone (or pull latest)
git clone https://github.com/Rohitz999/devops-cicd-app.git
cd devops-cicd-app

# Edit any file
nano src/main/resources/static/index.html

# Commit and push
git add .
git commit -m "Update landing page"
git push origin main
```

**That's it.** The pipeline takes over:
1. Jenkins builds new image `1.0.0-<build#>`
2. Pushes to DockerHub
3. Triggers GitOps pipeline
4. ArgoCD syncs to K3s
5. Live app updates in ~2 minutes
6. You get an email notification

### Watch the Pipeline

- **Jenkins:** [https://jenkins.mechnomax.co.in/job/devops-cicd-app](https://jenkins.mechnomax.co.in/job/devops-cicd-app)
- **ArgoCD:** [https://argocd.mechnomax.co.in](https://argocd.mechnomax.co.in)
- **Live App:** [https://app.mechnomax.co.in](https://app.mechnomax.co.in)

### Roll Back

Rollback is done in the [GitOps repo](https://github.com/Rohitz999/devops-mega-gitops) by changing the image tag in `manifests/deployment.yaml` back to a previous version, then committing.

---

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `No plugin found for prefix 'sonar'` | Missing `sonar-maven-plugin` in `pom.xml` | Add plugin to `<build><plugins>` |
| `no space left on device` (Trivy) | Cache fills disk | Add `--skip-java-db-update` + cleanup cache |
| `cannot perform interactive login` | DockerHub creds wrong | Recreate `dockerhub-creds` as Username + Password |
| `403 No valid crumb` | Jenkins CSRF | Use Jenkins API token (already configured) |
| `400 buildWithParameters` | GitOps job missing `IMAGE_TAG` param | Add String Parameter in job config |
| App build fails | Local test failure | Run `mvn clean test` locally first |

**For full troubleshooting:** See the [GitOps repo README](https://github.com/Rohitz999/devops-mega-gitops#troubleshooting)

---

## Related Repositories

| Repo | Purpose |
|------|---------|
| **[devops-mega-gitops](https://github.com/Rohitz999/devops-mega-gitops)** | K8s manifests + ArgoCD config + GitOps pipeline |

**Documentation:**
- Setup guide: See the GitOps repo README
- Full architecture: See the GitOps repo README

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## Author

**Rohit Vishwakarma**
- GitHub: [@Rohitz999](https://github.com/Rohitz999)
- Email: rohitvishwakarma8082@gmail.com

---

## ⭐ Show Your Support

If this project helped you, give it a star ⭐ — it helps others discover it!

**Live Demo:** [https://app.mechnomax.co.in](https://app.mechnomax.co.in)
