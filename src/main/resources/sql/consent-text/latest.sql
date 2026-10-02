SELECT version, body, created_at
FROM consent_text
WHERE practitioner_id = :practitioner
ORDER BY version DESC
LIMIT 1
