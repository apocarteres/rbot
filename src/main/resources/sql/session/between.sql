SELECT id, practitioner_id, client_id, type_id, starts_at, ends_at, status, price_snapshot, cancelled_by, rescheduled_to
FROM session
WHERE practitioner_id = :practitioner AND starts_at >= :from AND starts_at < :to
ORDER BY starts_at, created_at
