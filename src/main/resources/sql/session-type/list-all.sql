SELECT id, title, duration_minutes, price, buffer_minutes, format, active
FROM session_type
WHERE practitioner_id = :practitioner
ORDER BY created_at, title
