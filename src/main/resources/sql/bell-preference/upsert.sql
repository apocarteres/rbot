INSERT INTO bell_preference (account_id, sound, updated_at)
VALUES (:account, :sound, :updatedAt)
ON CONFLICT (account_id) DO UPDATE SET sound = EXCLUDED.sound, updated_at = EXCLUDED.updated_at
