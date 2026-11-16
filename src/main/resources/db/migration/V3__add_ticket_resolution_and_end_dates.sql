ALTER TABLE tickets ADD COLUMN resolution_note TEXT;
ALTER TABLE tickets ADD COLUMN closed_at TIMESTAMP;
ALTER TABLE tickets ADD COLUMN withdrawn_at TIMESTAMP;
