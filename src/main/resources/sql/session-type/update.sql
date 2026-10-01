UPDATE session_type
SET title = :title, duration_minutes = :durationMinutes, price = :price, format = :format, first_visit = :firstVisit,
  active = :active
WHERE id = :id
