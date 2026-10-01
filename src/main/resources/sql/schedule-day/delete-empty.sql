DELETE FROM schedule_day_interval
WHERE practitioner_id = :practitioner AND cardinality(session_types) = 0
