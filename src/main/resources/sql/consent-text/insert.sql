INSERT INTO consent_text (id, practitioner_id, version, body, created_at)
VALUES (:id, :practitioner, :version, :body, :createdAt)
ON CONFLICT (practitioner_id, version) DO NOTHING
