INSERT INTO telegram_polling_lease AS lease (id, instance_id, started_at, renewed_at)
VALUES (1, :instance, :startedAt, :now)
ON CONFLICT (id) DO UPDATE
SET instance_id = EXCLUDED.instance_id, started_at = EXCLUDED.started_at, renewed_at = EXCLUDED.renewed_at
WHERE lease.instance_id = EXCLUDED.instance_id OR lease.started_at < EXCLUDED.started_at OR lease.renewed_at < :stale
