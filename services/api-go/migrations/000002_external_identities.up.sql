CREATE TABLE external_identities (
  issuer text NOT NULL CHECK (issuer <> ''),
  subject text NOT NULL CHECK (subject <> ''),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (issuer, subject)
);

CREATE INDEX idx_external_identities_user_id ON external_identities(user_id);
