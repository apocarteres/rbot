SELECT lower(occupied) AS starts, upper(occupied) AS ends
FROM session
WHERE practitioner_id = :practitioner AND status IN ('BOOKED', 'REQUESTED') AND occupied && tstzrange(:from, :to, '[)')
