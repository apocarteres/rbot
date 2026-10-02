UPDATE telegram_polling_lease
SET renewed_at = :released
WHERE id = 1 AND instance_id = :instance
