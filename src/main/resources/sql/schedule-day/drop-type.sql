UPDATE schedule_day_interval
SET session_types = array_remove(session_types, CAST(:type AS UUID))
WHERE CAST(:type AS UUID) = ANY (session_types)
