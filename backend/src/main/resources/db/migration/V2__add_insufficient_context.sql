-- Alter Messages Table to support text grounding gap tracking
ALTER TABLE messages ADD COLUMN insufficient_context BOOLEAN DEFAULT FALSE NOT NULL;
