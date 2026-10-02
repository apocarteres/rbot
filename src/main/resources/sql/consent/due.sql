SELECT c.practitioner_id
FROM client c
WHERE c.telegram_user_id = :telegramUserId
  AND coalesce((SELECT max(s.version) FROM consent s WHERE s.client_id = c.id), 0)
    < coalesce((SELECT max(t.version) FROM consent_text t WHERE t.practitioner_id = c.practitioner_id), 1)
ORDER BY c.created_at
