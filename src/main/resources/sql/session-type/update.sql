UPDATE session_type
SET title = :title, duration_minutes = :durationMinutes, price = :price, buffer_minutes = :bufferMinutes, format = :format,
  active = :active
WHERE id = :id AND deleted_at IS NULL
