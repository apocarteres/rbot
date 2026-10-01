UPDATE session_type
SET deleted_at = :at, active = FALSE
WHERE practitioner_id = :practitioner AND id = :id AND deleted_at IS NULL
