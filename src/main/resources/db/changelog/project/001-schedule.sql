CREATE TABLE practice_settings (
  id SMALLINT PRIMARY KEY CHECK (id = 1),
  zone VARCHAR(64) NOT NULL,
  lead_minutes INT CHECK (lead_minutes BETWEEN 0 AND 10080),
  horizon_days INT CHECK (horizon_days BETWEEN 1 AND 365),
  slot_step_minutes INT CHECK (slot_step_minutes BETWEEN 5 AND 240),
  buffer_minutes INT CHECK (buffer_minutes BETWEEN 0 AND 240),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO practice_settings (id, zone, updated_at) VALUES (1, 'Europe/Moscow', now());

CREATE TABLE work_interval (
  id UUID PRIMARY KEY,
  weekday SMALLINT NOT NULL CHECK (weekday BETWEEN 1 AND 7),
  starts TIME NOT NULL,
  ends TIME NOT NULL,
  CHECK (starts < ends)
);

CREATE INDEX work_interval_weekday ON work_interval (weekday);

CREATE TABLE schedule_day (
  day DATE PRIMARY KEY,
  closed BOOLEAN NOT NULL,
  note VARCHAR(200)
);

CREATE TABLE schedule_day_interval (
  id UUID PRIMARY KEY,
  day DATE NOT NULL REFERENCES schedule_day (day) ON DELETE CASCADE,
  starts TIME NOT NULL,
  ends TIME NOT NULL,
  CHECK (starts < ends)
);

CREATE INDEX schedule_day_interval_day ON schedule_day_interval (day);

CREATE TABLE session_type (
  id UUID PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  duration_minutes INT NOT NULL CHECK (duration_minutes BETWEEN 15 AND 480),
  price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
  format VARCHAR(16) NOT NULL CHECK (format IN ('IN_PERSON', 'ONLINE')),
  first_visit BOOLEAN NOT NULL,
  active BOOLEAN NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO session_type (id, title, duration_minutes, price, format, first_visit, active, created_at) VALUES
  ('6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0001', 'Разовая консультация', 90, 0, 'IN_PERSON', TRUE, FALSE, now()),
  ('6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0002', 'Психотерапия очно', 60, 0, 'IN_PERSON', FALSE, FALSE, now()),
  ('6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0003', 'Психотерапия онлайн', 60, 0, 'ONLINE', FALSE, FALSE, now());
