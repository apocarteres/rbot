UPDATE work_interval SET session_types = ARRAY(SELECT id FROM session_type ORDER BY id) WHERE session_types = '{}';

UPDATE schedule_day_interval SET session_types = ARRAY(SELECT id FROM session_type ORDER BY id) WHERE session_types = '{}';
