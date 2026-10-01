SELECT id, client_id, type_id, starts_at, ends_at, status, price_snapshot
FROM session
WHERE client_id = :clientId AND status = 'BOOKED' AND ends_at > :now
ORDER BY starts_at
