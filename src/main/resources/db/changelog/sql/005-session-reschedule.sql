ALTER TABLE session ADD COLUMN rescheduled_to UUID REFERENCES session (id);

CREATE INDEX session_starts ON session (starts_at);
