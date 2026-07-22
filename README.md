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
