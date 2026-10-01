INSERT INTO schedule_day (practitioner_id, day, closed, note)
VALUES (:practitioner, :day, :closed, :note)
ON CONFLICT (practitioner_id, day) DO UPDATE SET closed = EXCLUDED.closed, note = EXCLUDED.note
