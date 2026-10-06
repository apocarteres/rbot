CREATE TABLE open_slot (
  practitioner_id UUID NOT NULL,
  start_at TIMESTAMP WITH TIME ZONE NOT NULL,
  opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (practitioner_id, start_at)
);
