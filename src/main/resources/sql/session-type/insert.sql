INSERT INTO session_type (id, title, duration_minutes, price, buffer_minutes, format, first_visit, active, created_at)
VALUES (:id, :title, :durationMinutes, :price, :bufferMinutes, :format, FALSE, :active, :createdAt)
