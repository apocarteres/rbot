UPDATE session_type
SET deleted_at = :at, active = FALSE
WHERE id = :id AND deleted_at IS NULL
