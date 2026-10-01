ALTER TABLE session_type ADD COLUMN buffer_minutes INT NOT NULL DEFAULT 0 CHECK (buffer_minutes BETWEEN 0 AND 240);

UPDATE session_type SET buffer_minutes = COALESCE((SELECT buffer_minutes FROM practice_settings WHERE id = 1), 0);

ALTER TABLE session_type ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE session_type ALTER COLUMN first_visit SET DEFAULT FALSE;

DELETE FROM session_type t
WHERE t.id IN ('6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0001', '6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0002', '6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0003')
  AND t.price = 0 AND NOT t.active AND NOT EXISTS (SELECT 1 FROM session s WHERE s.type_id = t.id);

UPDATE work_interval w SET session_types = ARRAY(SELECT x FROM unnest(w.session_types) x WHERE x IN (SELECT id FROM session_type));

UPDATE schedule_day_interval d SET session_types = ARRAY(SELECT x FROM unnest(d.session_types) x WHERE x IN (SELECT id FROM session_type));

DELETE FROM work_interval WHERE cardinality(session_types) = 0;

DELETE FROM schedule_day_interval WHERE cardinality(session_types) = 0;
