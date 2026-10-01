DELETE FROM client_invite
WHERE client_id = :clientId AND redeemed_at IS NULL
