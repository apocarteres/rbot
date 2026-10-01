INSERT INTO client (id, practitioner_id, account_id, status, created_at)
VALUES (:id, :practitioner, :accountId, 'ACTIVE', :createdAt)
ON CONFLICT (practitioner_id, account_id) DO NOTHING
