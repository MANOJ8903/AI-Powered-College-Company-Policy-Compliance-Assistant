-- Create Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create Documents Table
CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_name VARCHAR(255) NOT NULL,
    doc_type VARCHAR(255) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    role_scope VARCHAR(255) NOT NULL,
    department_scope VARCHAR(255) NOT NULL,
    chroma_collection_ref VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create Conversations Table
CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create Messages Table
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender VARCHAR(50) NOT NULL, -- "USER" or "ASSISTANT"
    content LONGTEXT NOT NULL,
    citations_json LONGTEXT,
    confidence DOUBLE NOT NULL,
    escalate_flag BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Seed Default Administrator (username: admin, email: admin@policyguardian.com, password: adminpassword)
INSERT INTO users (name, email, password_hash, role, department)
VALUES ('admin', 'admin@policyguardian.com', '$2a$10$gDW896F.R4or8BgcX7M8B.LZPnOC/7L7/TgniXf8gs15LRTgJHh..', 'ADMIN', 'HR');
