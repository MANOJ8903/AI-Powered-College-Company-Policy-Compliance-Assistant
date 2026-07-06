-- Supabase / Postgres initialization script for Policy Guardian
-- Run this in the Supabase SQL editor (Database -> SQL Editor -> New Query)

-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Create Documents Table
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    doc_name VARCHAR(255) NOT NULL,
    doc_type VARCHAR(255) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    role_scope VARCHAR(255) NOT NULL,
    department_scope VARCHAR(255) NOT NULL,
    chroma_collection_ref VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Create Conversations Table
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    started_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create Messages Table
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    citations_json TEXT,
    confidence DOUBLE PRECISION NOT NULL,
    escalate_flag BOOLEAN DEFAULT FALSE NOT NULL,
    insufficient_context BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Optional: Seed Default Administrator (username: admin, email: admin@policyguardian.com)
-- Password hash below is bcrypt for 'adminpassword' used in local seeds; replace if you want a different password.
INSERT INTO users (name, email, password_hash, role, department)
SELECT 'admin', 'admin@policyguardian.com', '$2a$10$gDW896F.R4or8BgcX7M8B.LZPnOC/7L7/TgniXf8gs15LRTgJHh..', 'ADMIN', 'HR'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE name = 'admin');

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);

-- Grant minimal privileges to the anon/public role is not required for Supabase internal access; adjust if necessary.

-- Done
