INSERT INTO schedule_day_interval (id, practitioner_id, day, starts, ends, session_types)
VALUES (:id, :practitioner, :day, :starts, :ends, CAST(string_to_array(:types, ',') AS UUID[]))
