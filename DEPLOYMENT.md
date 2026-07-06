# Deployment Guide — AI Policy Guardian

This guide describes how to deploy the full-stack containerized **AI Policy Guardian** compliance assistant to an AWS EC2 production instance.

---

## 1. AWS EC2 Instance Provisioning

1. Log into your **AWS Management Console**.
2. Navigate to **EC2 Dashboard** and click **Launch Instance**.
3. Configure the instance properties:
   - **Name**: `AI-Policy-Guardian-Server`
   - **Amazon Machine Image (AMI)**: `Ubuntu Server 22.04 LTS` (64-bit x86)
   - **Instance Type**: `t3.medium` (Minimum recommended: 2 vCPUs, 4GB RAM to compile React assets and Java cleanly inside Docker).
   - **Key Pair**: Select or create a key pair (`.pem`) to access via SSH.
4. Configure **Security Group (Firewall)**:
   Create a security group allowing the following inbound traffic rules:
   
   | Protocol | Port Range | Source | Description |
   | :--- | :--- | :--- | :--- |
   | TCP (SSH) | 22 | My IP (or `0.0.0.0/0`) | Administrator terminal access |
   | TCP (HTTP) | 80 | `0.0.0.0/0` | Access the Nginx React frontend |
   | TCP (API) | 8080 | `0.0.0.0/0` | REST requests forwarding to backend |

---

## 2. Docker & Docker Compose Installation on Ubuntu

Once the EC2 instance is running, connect to it using SSH:
```bash
ssh -i "your-key.pem" ubuntu@your-ec2-public-ip
```

Update system dependencies and install the Docker engine:
```bash
# Update local packages index
sudo apt-get update -y
sudo apt-get upgrade -y

# Install Docker dependencies
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up the stable repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Verify Docker service is active
sudo systemctl status docker --no-pager
```

To run Docker without prefixing `sudo`, add the `ubuntu` user to the docker group:
```bash
sudo usermod -aG docker ubuntu
# Log out and log back in for the changes to take effect
exit
```

---

## 3. Clone Repository & Setup Environments

Re-connect to the EC2 instance:
```bash
ssh -i "your-key.pem" ubuntu@your-ec2-public-ip
```

Clone the compliance assistant codebase and move into the root folder:
```bash
git clone <your-repository-url>
cd "AI-Powered College & Company Policy Compliance Assistant"
```

Configure the environment variables using the template:
```bash
cp .env.example .env
nano .env
```
Edit the `.env` file and set:
- **`GEMINI_API_KEY`**: Your Google AI Studio API Key.
- **`JWT_SECRET`**: A strong, randomly generated string key.
- **`DB_ROOT_PASSWORD`** & **`DB_PASSWORD`**: Set secure passwords for production MySQL.

---

## 4. Spin up the Production Stack

Start the orchestration stack. Docker will automatically build the React assets served by Nginx, package the Spring Boot JAR, and link them to MySQL and ChromaDB:
```bash
docker compose up -d --build
```

Verify that all four containers are running and healthy:
```bash
docker compose ps
```

Monitor compilation logs and server execution output:
```bash
docker compose logs -f
```

---

## 5. First Login and Production Verification

1. Open your browser and navigate to `http://your-ec2-public-ip/` (Port 80).
2. Log in using the default seeded admin account:
   - **Username**: `admin`
   - **Password**: `adminpassword`
3. Immediately navigate to the **Admin Dashboard -> User Directory** or run a SQL query to update the default password for production security.
4. Upload a policy PDF/Word document, set visibility scope, and ask grounding compliance questions in the Chat Console to verify.