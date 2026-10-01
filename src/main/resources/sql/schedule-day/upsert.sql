INSERT INTO schedule_day (day, closed, note)
VALUES (:day, :closed, :note)
ON CONFLICT (day) DO UPDATE SET closed = EXCLUDED.closed, note = EXCLUDED.note
