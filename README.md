# 🏦 SecureBank — Three-Tier AWS DevOps Project

<p>
<img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white"/>
<img src="https://img.shields.io/badge/DOCKER-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
<img src="https://img.shields.io/badge/JENKINS-D24939?style=for-the-badge&logo=jenkins&logoColor=white"/>
<img src="https://img.shields.io/badge/ANSIBLE-EE0000?style=for-the-badge&logo=ansible&logoColor=white"/>
<img src="https://img.shields.io/badge/PROJECT-THREE--TIER--DEVOPS-007ACC?style=for-the-badge"/>
<img src="https://img.shields.io/badge/STATUS-DEPLOYED-32CD32?style=for-the-badge"/>
</p>

Designed and deployed **SecureBank**, a three-tier simulated banking application on AWS using **Amazon EC2, Application Load Balancer, Docker Compose, Jenkins CI/CD, and Ansible automation**.

The project demonstrates practical DevOps concepts including **AWS networking, EC2 deployment, HTTPS configuration, containerization, reverse proxy, CI/CD automation, configuration management, SSH-based deployment, and application monitoring**.

> **Note:** SecureBank is an educational demonstration project. It uses fictional banking data and simulated transfers. It is not a real banking, payment, or production-certified financial system. Do not enter real banking credentials or financial information.

---

## 🌐 Live Application


- **GitHub Repository:** [SecureBank — Three-Tier AWS DevOps](https://github.com/Mohamedthoufiq31/securebank-3-tier-devops)

The live application is available only while the required AWS resources are running.

---

## 📌 Project Overview

In this project, I learnt and implemented how to:

✅ Develop a full-stack banking simulation application

✅ Build a frontend using React and TypeScript

✅ Develop REST APIs using Spring Boot and Java 21

✅ Store application data using PostgreSQL

✅ Containerize frontend, backend, and database services using Docker

✅ Manage multiple containers using Docker Compose

✅ Launch and configure AWS EC2 instances

✅ Deploy the application on an AWS EC2 instance

✅ Configure an Application Load Balancer

✅ Configure HTTPS using AWS Certificate Manager (ACM)

✅ Connect a custom domain using Amazon Route 53

✅ Configure Nginx as a web server and reverse proxy

✅ Automate server configuration using Ansible

✅ Create a Jenkins CI/CD pipeline for remote deployment

✅ Deploy application updates using SSH and Docker Compose

This project is designed for beginners who want practical experience with **AWS, Docker, Jenkins, Ansible, and end-to-end DevOps deployment workflows**.

---

## 🏗️ Architecture

<p align="center">
  <img src="images/01-Architecture.png" width="950" alt="SecureBank AWS three-tier architecture"/>
</p>

### Architecture Overview

The project uses separate EC2 instances for the application and Jenkins.

| Component | Responsibility |
|-----------|----------------|
| Amazon Route 53 | Resolves the custom domain |
| Application Load Balancer | Receives and routes incoming web traffic |
| AWS Certificate Manager | Provides the TLS certificate for HTTPS |
| Application EC2 | Runs the application using Docker Compose |
| Jenkins EC2 | Runs the CI/CD pipeline |
| Nginx | Serves the frontend and proxies API requests |
| Spring Boot | Handles backend APIs and application logic |
| PostgreSQL | Stores application data |
| Ansible | Automates server configuration and deployment tasks |
| GitHub | Stores application source code |

### Application Request Flow

```text
User Browser
     |
     v
Amazon Route 53
     |
     v
Application Load Balancer
     |
     +---- HTTP :80  ---> Redirect to HTTPS :443
     |
     +---- HTTPS :443 ---> Application Target Group
                              |
                              v
                         Application EC2
                              |
                         Docker Compose
                              |
                 +------------+------------+
                 |            |            |
                 v            v            v
              Nginx       Spring Boot   PostgreSQL
              Frontend     Backend       Database
