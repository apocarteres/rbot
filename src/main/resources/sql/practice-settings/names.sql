SELECT practitioner_id, display_name
FROM practice_settings
WHERE practitioner_id IN (:practitioners)
