CREATE TABLE bell_preference (
  account_id UUID PRIMARY KEY,
  sound BOOLEAN NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
