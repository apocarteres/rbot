SELECT telegram_user_id
FROM client
WHERE id = :id AND telegram_user_id IS NOT NULL
