INSERT INTO schedule_day_interval (id, day, starts, ends, session_types)
VALUES (:id, :day, :starts, :ends, CAST(string_to_array(:types, ',') AS UUID[]))
