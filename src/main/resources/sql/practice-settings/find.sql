SELECT zone, lead_minutes, horizon_days, slot_step_minutes, display_name
FROM practice_settings
WHERE practitioner_id = :practitioner
