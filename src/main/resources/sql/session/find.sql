SELECT id, client_id, type_id, starts_at, ends_at, status, price_snapshot
FROM session
WHERE id = :id AND client_id = :clientId
