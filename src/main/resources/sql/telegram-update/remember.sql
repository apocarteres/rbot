INSERT INTO telegram_update (update_id, received_at)
VALUES (:updateId, :at)
ON CONFLICT (update_id) DO NOTHING
