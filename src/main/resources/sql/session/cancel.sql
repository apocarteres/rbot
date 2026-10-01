UPDATE session
SET status = 'CANCELLED', cancelled_at = :at, cancelled_by = :by
WHERE id = :id AND client_id = :clientId AND status = 'BOOKED'
