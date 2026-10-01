SELECT c.id, c.practitioner_id, c.label, c.account_id, c.telegram_user_id IS NOT NULL AS telegram, c.status,
  (SELECT max(i.expires_at) FROM client_invite i WHERE i.client_id = c.id AND i.redeemed_at IS NULL) AS invite_expires_at
FROM client c
WHERE c.account_id = :accountId
ORDER BY c.created_at
