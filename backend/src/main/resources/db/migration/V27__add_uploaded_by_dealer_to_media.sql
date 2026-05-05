ALTER TABLE media_files
    ADD COLUMN IF NOT EXISTS uploaded_by_dealer_id UUID REFERENCES dealers(id) ON DELETE SET NULL;
