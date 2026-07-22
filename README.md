# 🎓 AI-Powered College & Company Policy Compliance Assistant

An intelligent AI assistant that helps students, staff, and employees quickly understand and query college or company policies, rules, and compliance documents using Retrieval-Augmented Generation (RAG). Instead of manually searching through lengthy PDF handbooks, users can simply ask questions in natural language and get accurate, context-aware answers backed by the source documents.

---

## 🚀 Overview

Organizations often have policy documents (college rulebooks, HR handbooks, compliance manuals) that are long, dense, and hard to navigate. This project solves that by:

- Ingesting policy documents (PDFs) into a vector database
- Using semantic search to retrieve the most relevant policy sections for a user's question
- Generating clear, accurate answers using an LLM, grounded strictly in the source document
- Reducing dependence on manual lookup or admin staff for common policy queries

---

## ✨ Features

- 📄 **Document Ingestion** — Upload and process college/company policy PDFs
- 🔍 **Semantic Search (RAG)** — Retrieves relevant policy clauses using vector embeddings
- 🤖 **AI-Powered Q&A** — Natural language answers grounded in actual policy text
- 🗄️ **Persistent Storage** — Supabase/PostgreSQL backend for structured data and metadata
- 🐳 **Dockerized Setup** — One-command local development with Docker Compose
- ☁️ **Cloud Deployment Ready** — Pre-configured for Render deployment
- 🔐 **Environment-based Config** — Secure API key and secret management via `.env`

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React / TypeScript |
| **Backend** | Node.js / Express (or FastAPI) |
| **Database** | Supabase (PostgreSQL) |
| **AI/LLM** | Groq API (Llama 3.3 70B) |
| **Vector Search** | Supabase Vector / pgvector |
| **Containerization** | Docker & Docker Compose |
| **Deployment** | Render |

---

## 📁 Project Structure
AI-Powered-College-Company-Policy-Compliance-Assistant/
├── backend/ # API server, RAG logic, DB models
├── frontend/ # Client application (React/TS)
├── .env.example # Environment variable template
├── docker-compose.yml # Multi-container local dev setup
├── render.yaml # Render deployment configuration
├── setup_database.sql # Database schema setup
├── supabase_init.sql # Supabase initialization script
├── sample_college_rules.pdf # Sample policy document for testing
├── run.bat # Quick-start script (Windows)
├── DEPLOYMENT.md # General deployment guide
├── DEPLOY_RENDER.md # Render-specific deployment steps
└── PROGRESS.md # Development progress log


---

## ⚙️ Getting Started

### Prerequisites
- Node.js (v18+)
- Docker & Docker Compose
- Supabase account (or local PostgreSQL with pgvector)
- Groq API key

### 1. Clone the repository
```bash
git clone https://github.com/MANOJ8903/AI-Powered-College-Company-Policy-Compliance-Assistant.git
cd AI-Powered-College-Company-Policy-Compliance-Assistant
```

### 2. Configure environment variables
```bash
cp .env.example .env
```
Fill in your credentials:
GROQ_API_KEY=your_groq_api_key
SUPABASE_URL=your_supabase_url
SUPABASE_KEY=your_supabase_key
DATABASE_URL=your_database_connection_string


### 3. Set up the database
```bash
psql -f setup_database.sql
psql -f supabase_init.sql
```

### 4. Run with Docker
```bash
docker-compose up --build
```

Or run locally without Docker (Windows):
```bash
run.bat
```

The app will be available at `http://localhost:3000` (frontend) and `http://localhost:5000` (backend API).

---

## ☁️ Deployment

This project is pre-configured for deployment on **Render** using `render.yaml`.
See [`DEPLOY_RENDER.md`](./DEPLOY_RENDER.md) for step-by-step deployment instructions, and [`DEPLOYMENT.md`](./DEPLOYMENT.md) for general deployment notes.

---

## 📖 Usage

1. Upload a policy document (e.g. `sample_college_rules.pdf`) through the ingestion endpoint/UI
2. The document is chunked, embedded, and stored in the vector database
3. Ask a question like *"What is the attendance policy for final-year students?"*
4. The assistant retrieves the relevant policy clauses and generates a grounded answer

---

## 🗺️ Roadmap

- [ ] Multi-document comparison
- [ ] Role-based access (student/staff/admin views)
- [ ] Chat history persistence per user
- [ ] Support for DOCX/HTML policy sources

See [`PROGRESS.md`](./PROGRESS.md) for detailed development status.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome. Feel free to open an issue or submit a pull request.

---

## 📄 License

This project is licensed under the MIT License.

---

## 👤 Author

**Manoj**
B.Tech Information Technology
GitHub: [@MANOJ8903](https://github.com/MANOJ8903)### 3. Set up the database
```bash
psql -f setup_database.sql
psql -f supabase_init.sql
```

### 4. Run with Docker
```bash
docker-compose up --build
```

Or run locally without Docker (Windows):
```bash
run.bat
```

The app will be available at `http://localhost:3000` (frontend) and `http://localhost:5000` (backend API).

---

## ☁️ Deployment

This project is pre-configured for deployment on **Render** using `render.yaml`.
See [`DEPLOY_RENDER.md`](./DEPLOY_RENDER.md) for step-by-step deployment instructions, and [`DEPLOYMENT.md`](./DEPLOYMENT.md) for general deployment notes.

---

## 📖 Usage

1. Upload a policy document (e.g. `sample_college_rules.pdf`) through the ingestion endpoint/UI
2. The document is chunked, embedded, and stored in the vector database
3. Ask a question like *"What is the attendance policy for final-year students?"*
4. The assistant retrieves the relevant policy clauses and generates a grounded answer

---

## 🗺️ Roadmap

- [ ] Multi-document comparison
- [ ] Role-based access (student/staff/admin views)
- [ ] Chat history persistence per user
- [ ] Support for DOCX/HTML policy sources

See [`PROGRESS.md`](./PROGRESS.md) for detailed development status.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome. Feel free to open an issue or submit a pull request.

---

## 📄 License

This project is licensed under the MIT License.

---

## 👤 Author

**Manoj Kumar M**
B.Tech Information Technology
GitHub: [@MANOJ8903](https://github.com/MANOJ8903)
