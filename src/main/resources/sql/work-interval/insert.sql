INSERT INTO work_interval (id, weekday, starts, ends, session_types)
VALUES (:id, :weekday, :starts, :ends, CAST(string_to_array(:types, ',') AS UUID[]))
