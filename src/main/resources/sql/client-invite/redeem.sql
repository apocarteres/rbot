UPDATE client_invite
SET redeemed_at = :now
WHERE token_hash = :tokenHash AND redeemed_at IS NULL AND expires_at > :now
RETURNING client_id
