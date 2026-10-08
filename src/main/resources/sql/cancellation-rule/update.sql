UPDATE cancellation_rule
SET min_hours = :minHours, allowed = :allowed, client_text = :clientText, updated_at = :updatedAt
WHERE practitioner_id = :practitioner AND id = :id
  AND NOT EXISTS (
    SELECT 1 FROM cancellation_rule other
    WHERE other.practitioner_id = :practitioner AND other.min_hours = :minHours AND other.id <> :id
  )
