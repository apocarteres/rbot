INSERT INTO client (id, account_id, status, created_at)
VALUES (:id, :accountId, 'ACTIVE', :createdAt)
ON CONFLICT (account_id) DO NOTHING
