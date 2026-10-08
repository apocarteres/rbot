INSERT INTO cancellation_rule (id, practitioner_id, min_hours, allowed, client_text, updated_at)
VALUES (:id, :practitioner, :minHours, :allowed, :clientText, :updatedAt)
ON CONFLICT (practitioner_id, min_hours) DO NOTHING
