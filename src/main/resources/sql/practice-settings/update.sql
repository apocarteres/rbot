UPDATE practice_settings
SET zone = :zone, lead_minutes = :leadMinutes, horizon_days = :horizonDays, slot_step_minutes = :slotStepMinutes,
  updated_at = :updatedAt
WHERE id = 1
