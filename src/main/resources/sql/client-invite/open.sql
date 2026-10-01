SELECT client_id
FROM client_invite
WHERE token_hash = :tokenHash AND redeemed_at IS NULL AND expires_at > :now
