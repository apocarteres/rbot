package com.yanapaderina.rbot.bell.internal.app;

import com.yanapaderina.rbot.bell.internal.data.BellPreferenceDao;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RBOT-FEAT-020
@Service
public class BellPreferences {

  private final BellPreferenceDao preferences;
  private final Clock clock;

  BellPreferences(BellPreferenceDao preferences, Clock clock) {
    this.preferences = preferences;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public boolean sound(UUID account) {
    return preferences.sound(account).orElse(false);
  }

  @Transactional
  public boolean changeSound(UUID account, boolean sound) {
    preferences.upsert(account, sound, clock.instant());
    return sound;
  }
}
