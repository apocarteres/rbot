CREATE TABLE session (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL REFERENCES client (id),
  type_id UUID NOT NULL REFERENCES session_type (id),
  starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
  ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
  occupied TSTZRANGE NOT NULL,
  status VARCHAR(16) NOT NULL CHECK (status IN ('REQUESTED', 'BOOKED', 'DECLINED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
  price_snapshot NUMERIC(12, 2) NOT NULL CHECK (price_snapshot >= 0),
  policy_version INT,
  created_by UUID NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  cancelled_at TIMESTAMP WITH TIME ZONE,
  cancelled_by UUID,
  CHECK (starts_at < ends_at),
  CHECK (lower(occupied) = starts_at AND upper(occupied) >= ends_at),
  CONSTRAINT session_no_overlap EXCLUDE USING gist (occupied WITH &&) WHERE (status IN ('BOOKED', 'REQUESTED'))
);

CREATE INDEX session_client ON session (client_id, starts_at);
