CREATE TABLE cancellation_policy (
  practitioner_id UUID PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE cancellation_rule (
  id UUID PRIMARY KEY,
  practitioner_id UUID NOT NULL REFERENCES cancellation_policy (practitioner_id) ON DELETE CASCADE,
  min_hours INT NOT NULL CHECK (min_hours >= 0 AND min_hours <= 8760),
  allowed BOOLEAN NOT NULL,
  client_text VARCHAR(500) NOT NULL CHECK (length(client_text) > 0),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT cancellation_rule_hours UNIQUE (practitioner_id, min_hours)
);
