CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE practice_settings ADD COLUMN practitioner_id UUID;

ALTER TABLE practice_settings ADD COLUMN display_name VARCHAR(100);

ALTER TABLE work_interval ADD COLUMN practitioner_id UUID;

ALTER TABLE schedule_day ADD COLUMN practitioner_id UUID;

ALTER TABLE schedule_day_interval ADD COLUMN practitioner_id UUID;

ALTER TABLE session_type ADD COLUMN practitioner_id UUID;

ALTER TABLE client ADD COLUMN practitioner_id UUID;

ALTER TABLE session ADD COLUMN practitioner_id UUID;
