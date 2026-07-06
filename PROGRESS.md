# PROGRESS.md — AI Policy Guardian

A compliance assistant for querying college and company policy documents using a RAG (Retrieval-Augmented Generation) pipeline.

---

## Workspace Structure

The project is structured as a multi-container stack comprising:
```
AI-Powered College & Company Policy Compliance Assistant/
├── .env.example              # Core environment configuration template
├── docker-compose.yml        # Docker stack layout (MySQL, Chroma, backend, frontend)
├── PROGRESS.md               # Setup instructions, architecture overview & task list
├── backend/
│   ├── Dockerfile            # Multi-stage JDK 21 jar builder
│   ├── pom.xml               # Maven dependencies (WebFlux, JPA, JJWT, PDFBox, POI)
│   └── src/
│       ├── main/java/com/policyguardian/
│       │   ├── controller/   # REST endpoints (Auth, Ingestion, Query, Analytics)
│       │   ├── service/      # Grounding logic, parsers, and DB integration services
│       │   ├── repository/   # Spring Data repositories
│       │   ├── model/        # JPA Entities (User, PolicyDocument, QueryLog)
│       │   ├── dto/          # Java 21 Records / JSON deserialization DTOs
│       │   └── security/     # JWT Provider, authentication interceptors, and CORS configs
│       └── test/java/com/policyguardian/
│           ├── PolicyGuardianApplicationTests.java
│           └── service/      # Pipeline unit and Mockito mock tests
└── frontend/
    ├── Dockerfile            # Nginx server serving compiled Vite static bundle
    ├── package.json          # Node modules definitions (Axios, React Router, MUI)
    ├── index.html            # Vite template entry point
    └── src/
        ├── index.css         # Glassmorphic, custom dark CSS design token styles
        ├── App.tsx           # Global authentication state switcher & route manager
        ├── main.tsx          # React application bootstrapping
        ├── services/
        │   └── api.ts        # Axios backend client interface wrapper & TypeScript DTO interfaces
        └── pages/
            ├── Login.tsx     # Sign-up and Sign-in forms
            ├── Chat.tsx      # Policy queries, inline citations, and source overlay drawers
            └── AdminDashboard.tsx # Ingestion panels, documents registers, and HR escalated query logs
```
---
## Current Status

All phases are fully complete and operational:
- **Phase 1: Environment & Infra Config**: Complete.
- **Phase 2: User Authentication & Database**: Complete. (Using Lombok `1.18.46` for JDK 24 compiler support).
- **Phase 3: Doc Parsing & Vector Integration**: Complete.
- **Phase 4: RAG Chat Logic & Citations**: Complete.
- **Phase 5: React + Vite Frontend**: Complete. (Installed `axios` and Material-UI `@mui/material` base component elements).
- **Phase 6: Analytics & Polish**: Complete. (HR tickets dashboard and query charts).

---

## How to Run the Docker Compose Stack

### Prerequisites
- Install **Docker Desktop** on Windows.
- Ensure ports `3306` (MySQL), `8000` (ChromaDB), `8080` (Backend API), and `80` (Frontend client) are not bound by other host processes.

---

### Step 1: Environment Configuration
Create a `.env` file in the root directory by copying `.env.example` and adding your Gemini API Key:
```powershell
cp .env.example .env
```
Open `.env` and fill in:
`GEMINI_API_KEY=AIzaSy...` (your Google Studio key)

---

### Step 2: Build and Start All Containers
From the root workspace directory, run:
```powershell
docker-compose up --build -d
```
Docker will:
1. Spin up the MySQL database container.
2. Spin up the ChromaDB vector database container.
3. Build the Spring Boot container (packaging source jars and running checks).
4. Build the React client container (fetching NPM packages, bundling assets via Vite, and loading Nginx).

To monitor container status and startup logs, run:
```powershell
docker-compose logs -f
```

---

### Step 3: Verify the Deployment Ports
Open your browser and verify each container endpoint:
- **ChromaDB Vector Store**: `http://localhost:8000/api/v1/heartbeat` (Should output JSON timestamp).
- **React Frontend Application**: `http://localhost/` (Opens the Login panel served by Nginx on port 80).
- **Spring Boot REST APIs**: `http://localhost:8080/api/auth/...` (Handles API request routing).

---

### Step 4: Run Grounded Compliance Queries
1. Navigate to the UI at `http://localhost/`.
2. Log in directly using the **Default Seeded Admin User** credentials:
   - **Username**: `admin`
   - **Password**: `adminpassword`
   
   > [!WARNING]
   > **SECURITY WARNING**: These credentials are automatically seeded via Flyway database migrations (`V1__init_schema.sql`). For security, you must update the default admin password immediately in the database or via registration in production environments.

3. Under the **Admin Dashboard** tab, upload a policy document (`.pdf` or `.docx`), specify its visibilities, and submit. The system parses it, generates embeddings, and saves it in ChromaDB and MySQL.
4. Toggle to the **Compliance Chat** page. Ask policy queries; the assistant answers grounded in context with clickable source citation pills.
5. Log out, register a `STUDENT` account, and verify they cannot access the Admin dashboard.
