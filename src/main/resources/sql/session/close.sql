UPDATE session
SET status = :status, cancelled_at = :at, cancelled_by = :by
WHERE id = :id AND status = 'BOOKED'
