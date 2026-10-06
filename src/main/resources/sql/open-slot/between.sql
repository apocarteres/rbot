SELECT start_at
FROM open_slot
WHERE practitioner_id = :practitioner AND start_at >= :from AND start_at < :to
