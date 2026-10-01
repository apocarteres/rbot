SELECT id, practitioner_id, client_id, type_id, starts_at, ends_at, status, price_snapshot, cancelled_by, rescheduled_to
FROM session
WHERE id = :id
