SELECT lower(occupied) AS starts, upper(occupied) AS ends
FROM session
WHERE status IN ('BOOKED', 'REQUESTED') AND occupied && tstzrange(:from, :to, '[)')
