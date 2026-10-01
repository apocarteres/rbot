INSERT INTO work_interval (id, practitioner_id, weekday, starts, ends, session_types)
VALUES (:id, :practitioner, :weekday, :starts, :ends, CAST(string_to_array(:types, ',') AS UUID[]))
