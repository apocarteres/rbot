package com.yanapaderina.rbot.booking.internal.web;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// MVP-05, MVP-08, RBOT-FEAT-017
interface ClientScope {

  List<UUID> practices();

  UUID client(UUID practice);

  Set<UUID> clients();
}
