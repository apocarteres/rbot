SELECT id, client_id, type_id, starts_at, ends_at, status, price_snapshot, cancelled_by, rescheduled_to
FROM session
WHERE starts_at >= :from AND starts_at < :to
ORDER BY starts_at, created_at
