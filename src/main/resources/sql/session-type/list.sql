SELECT id, title, duration_minutes, price, format, first_visit, active
FROM session_type
ORDER BY first_visit DESC, created_at, title
