ALTER TABLE client ADD COLUMN label VARCHAR(100);

ALTER TABLE client ADD COLUMN telegram_user_id BIGINT UNIQUE;

CREATE TABLE client_invite (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL REFERENCES client (id) ON DELETE CASCADE,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  redeemed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX client_invite_client ON client_invite (client_id);

CREATE TABLE consent (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL REFERENCES client (id) ON DELETE CASCADE,
  version INT NOT NULL,
  channel VARCHAR(16) NOT NULL CHECK (channel IN ('TELEGRAM')),
  accepted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX consent_client ON consent (client_id);
