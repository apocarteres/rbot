DELETE FROM work_interval
WHERE practitioner_id = :practitioner AND cardinality(session_types) = 0
