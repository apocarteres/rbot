UPDATE client
SET telegram_user_id = :telegramUserId, status = 'ACTIVE'
WHERE id = :id
