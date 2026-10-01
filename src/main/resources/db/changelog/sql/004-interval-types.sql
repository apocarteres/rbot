ALTER TABLE work_interval ADD COLUMN session_types UUID[] NOT NULL DEFAULT '{}';

ALTER TABLE schedule_day_interval ADD COLUMN session_types UUID[] NOT NULL DEFAULT '{}';
