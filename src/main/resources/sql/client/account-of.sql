SELECT account_id
FROM client
WHERE id = :id AND account_id IS NOT NULL
