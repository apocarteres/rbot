INSERT INTO cancellation_policy (practitioner_id, created_at)
VALUES (:practitioner, :createdAt)
ON CONFLICT (practitioner_id) DO NOTHING
