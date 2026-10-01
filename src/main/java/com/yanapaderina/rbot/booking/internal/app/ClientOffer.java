package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.schedule.BookingTerms;
import com.yanapaderina.rbot.schedule.SessionType;
import java.util.List;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-017, ADR-0003
public record ClientOffer(BookingTerms terms, List<SessionType> types) {
}
