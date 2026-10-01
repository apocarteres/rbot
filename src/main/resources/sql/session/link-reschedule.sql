UPDATE session
SET rescheduled_to = :to
WHERE id = :id AND status = 'CANCELLED'
