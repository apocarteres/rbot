UPDATE schedule_day_interval
SET session_types = array_remove(session_types, CAST(:type AS UUID))
WHERE practitioner_id = :practitioner AND CAST(:type AS UUID) = ANY (session_types)
