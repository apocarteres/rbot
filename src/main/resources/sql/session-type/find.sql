SELECT id, title, duration_minutes, price, buffer_minutes, format, active
FROM session_type
WHERE id = :id AND deleted_at IS NULL
