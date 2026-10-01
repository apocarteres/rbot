INSERT INTO session (id, client_id, type_id, starts_at, ends_at, occupied, status, price_snapshot, created_by, created_at)
VALUES (:id, :clientId, :typeId, :startsAt, :endsAt, tstzrange(:startsAt, :occupiedUntil, '[)'), :status, :price, :createdBy,
  :createdAt)
ON CONFLICT DO NOTHING
