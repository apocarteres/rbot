CREATE TABLE platform_account (
  id UUID PRIMARY KEY,
  email VARCHAR(320) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  blocked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE platform_account_role (
  account_id UUID NOT NULL REFERENCES platform_account (id) ON DELETE CASCADE,
  role VARCHAR(64) NOT NULL,
  PRIMARY KEY (account_id, role)
);

CREATE TABLE platform_account_token (
  digest CHAR(64) PRIMARY KEY,
  account_id UUID NOT NULL REFERENCES platform_account (id) ON DELETE CASCADE,
  purpose VARCHAR(32) NOT NULL,
  email VARCHAR(320),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  used_at TIMESTAMP WITH TIME ZONE
);
