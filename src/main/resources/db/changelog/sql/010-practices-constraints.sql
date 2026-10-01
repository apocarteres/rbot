DELETE FROM practice_settings WHERE practitioner_id IS NULL;

ALTER TABLE practice_settings DROP CONSTRAINT practice_settings_pkey;

ALTER TABLE practice_settings DROP COLUMN id;

ALTER TABLE practice_settings ALTER COLUMN practitioner_id SET NOT NULL;

ALTER TABLE practice_settings ADD PRIMARY KEY (practitioner_id);

ALTER TABLE work_interval ALTER COLUMN practitioner_id SET NOT NULL;

CREATE INDEX work_interval_practitioner ON work_interval (practitioner_id, weekday);

ALTER TABLE schedule_day_interval DROP CONSTRAINT schedule_day_interval_day_fkey;

ALTER TABLE schedule_day DROP CONSTRAINT schedule_day_pkey;

ALTER TABLE schedule_day ALTER COLUMN practitioner_id SET NOT NULL;

ALTER TABLE schedule_day ADD PRIMARY KEY (practitioner_id, day);

ALTER TABLE schedule_day_interval ALTER COLUMN practitioner_id SET NOT NULL;

ALTER TABLE schedule_day_interval ADD CONSTRAINT schedule_day_interval_day_fkey FOREIGN KEY (practitioner_id, day)
  REFERENCES schedule_day (practitioner_id, day) ON DELETE CASCADE;

ALTER TABLE session_type ALTER COLUMN practitioner_id SET NOT NULL;

CREATE INDEX session_type_practitioner ON session_type (practitioner_id);

ALTER TABLE client DROP CONSTRAINT client_account_id_key;

ALTER TABLE client DROP CONSTRAINT client_telegram_user_id_key;

ALTER TABLE client ALTER COLUMN practitioner_id SET NOT NULL;

ALTER TABLE client ADD CONSTRAINT client_practice_account UNIQUE (practitioner_id, account_id);

ALTER TABLE client ADD CONSTRAINT client_practice_telegram UNIQUE (practitioner_id, telegram_user_id);

CREATE INDEX client_telegram ON client (telegram_user_id);

ALTER TABLE session ALTER COLUMN practitioner_id SET NOT NULL;

ALTER TABLE session DROP CONSTRAINT session_no_overlap;

ALTER TABLE session ADD CONSTRAINT session_no_overlap EXCLUDE USING gist (practitioner_id WITH =, occupied WITH &&)
  WHERE (status IN ('BOOKED', 'REQUESTED'));
