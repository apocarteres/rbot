SELECT d.day, d.closed, d.note, i.starts, i.ends, i.session_types
FROM schedule_day d
LEFT JOIN schedule_day_interval i ON i.day = d.day
WHERE d.day BETWEEN :from AND :to
ORDER BY d.day, i.starts
