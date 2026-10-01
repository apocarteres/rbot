SELECT weekday, starts, ends, session_types
FROM work_interval
WHERE practitioner_id = :practitioner
ORDER BY weekday, starts
