INSERT INTO practice_settings (practitioner_id, zone, lead_minutes, horizon_days, slot_step_minutes, display_name, updated_at)
VALUES (:practitioner, :zone, :leadMinutes, :horizonDays, :slotStepMinutes, :displayName, :updatedAt)
ON CONFLICT (practitioner_id) DO UPDATE SET zone = EXCLUDED.zone, lead_minutes = EXCLUDED.lead_minutes,
  horizon_days = EXCLUDED.horizon_days, slot_step_minutes = EXCLUDED.slot_step_minutes, display_name = EXCLUDED.display_name,
  updated_at = EXCLUDED.updated_at
