INSERT INTO open_slot (practitioner_id, start_at, opened_at)
VALUES (:practitioner, :start, :openedAt)
ON CONFLICT (practitioner_id, start_at) DO NOTHING
