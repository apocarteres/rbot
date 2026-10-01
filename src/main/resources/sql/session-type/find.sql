SELECT id, title, duration_minutes, price, format, first_visit, active
FROM session_type
WHERE id = :id
