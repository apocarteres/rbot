CREATE TABLE telegram_polling_lease (
  id SMALLINT PRIMARY KEY CHECK (id = 1),
  instance_id VARCHAR(64) NOT NULL,
  started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  renewed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
