SELECT id, min_hours, allowed, client_text
FROM cancellation_rule
WHERE practitioner_id = :practitioner AND id = :id
