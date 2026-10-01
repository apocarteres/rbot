UPDATE session
SET status = 'NO_SHOW'
WHERE id = :id AND status = 'BOOKED' AND starts_at <= :now
