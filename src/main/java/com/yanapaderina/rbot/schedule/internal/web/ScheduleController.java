package com.yanapaderina.rbot.schedule.internal.web;

import com.yanapaderina.rbot.schedule.internal.app.ScheduleAdministration;
import com.yanapaderina.rbot.schedule.internal.app.ScheduleDay;
import com.yanapaderina.rbot.schedule.internal.app.ScheduleRefused;
import com.yanapaderina.rbot.schedule.internal.app.SlotPreview;
import io.github.apocarteres.platform.auth.CurrentAccount;
import jakarta.validation.Valid;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// MVP-02, RBOT-FEAT-016, RBOT-FEAT-017, ADR-0003
@RestController
@RequestMapping("/api/cabinet/schedule")
class ScheduleController {

  private final ScheduleAdministration schedule;
  private final SlotPreview slots;

  ScheduleController(ScheduleAdministration schedule, SlotPreview slots) {
    this.schedule = schedule;
    this.slots = slots;
  }

  @GetMapping("/settings")
  Views.Settings settings() {
    return Views.Settings.of(schedule.settings(me()));
  }

  @PutMapping("/settings")
  Views.Settings changeSettings(@Valid @RequestBody Views.SettingsRequest request) {
    return Views.Settings.of(schedule.changeSettings(me(), request.domain(zone(request.zone()))));
  }

  @GetMapping("/week")
  List<Views.Weekday> week() {
    return Views.week(schedule.week(me()));
  }

  @PutMapping("/week/{weekday}")
  List<Views.Interval> replaceWeekday(@PathVariable int weekday, @Valid @RequestBody Views.Hours request) {
    return schedule.replaceWeekday(me(), weekday(weekday), request.intervals().stream().map(Views.Interval::domain).toList())
      .stream().map(Views.Interval::of).toList();
  }

  @GetMapping("/days")
  List<Views.Day> days(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return schedule.days(me(), from, to).values().stream().map(Views.Day::of).toList();
  }

  @PutMapping("/days/{date}")
  Views.Day setDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
    @Valid @RequestBody Views.DayRequest request) {
    return Views.Day.of(schedule.setDay(me(), new ScheduleDay(date, request.closed(), note(request.note()),
      request.intervals().stream().map(Views.Interval::domain).toList())));
  }

  @PostMapping("/days/closed")
  Views.Closed closeDays(@Valid @RequestBody Views.ClosedRange request) {
    return new Views.Closed(schedule.closeDays(me(), request.from(), request.to(), note(request.note())));
  }

  @DeleteMapping("/days/{date}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void clearDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    schedule.clearDay(me(), date);
  }

  @GetMapping("/types")
  List<Views.Type> types() {
    return schedule.types(me()).stream().map(Views.Type::of).toList();
  }

  @PostMapping("/types")
  @ResponseStatus(HttpStatus.CREATED)
  Views.Type createType(@Valid @RequestBody Views.TypeRequest request) {
    return Views.Type.of(schedule.createType(me(), request.domain(null)));
  }

  @PutMapping("/types/{id}")
  Views.Type changeType(@PathVariable UUID id, @Valid @RequestBody Views.TypeRequest request) {
    return Views.Type.of(schedule.changeType(me(), request.domain(id)));
  }

  @DeleteMapping("/types/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteType(@PathVariable UUID id) {
    schedule.deleteType(me(), id);
  }

  @GetMapping("/slots")
  List<Views.Slot> slots(@RequestParam UUID type, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return slots.free(me(), type, from, to).stream().map(Views.Slot::of).toList();
  }

  private static UUID me() {
    return CurrentAccount.id().orElseThrow();
  }

  private static DayOfWeek weekday(int value) {
    if (value < 1 || value > 7) {
      throw new ScheduleRefused(ScheduleRefused.RANGE, "День недели " + value + ": от 1 до 7");
    }
    return DayOfWeek.of(value);
  }

  private static ZoneId zone(String value) {
    try {
      return ZoneId.of(value);
    } catch (DateTimeException unknown) {
      throw new ScheduleRefused(ScheduleRefused.SETTINGS, "Неизвестный часовой пояс " + value);
    }
  }

  private static String note(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    if (value.length() > 200) {
      throw new ScheduleRefused(ScheduleRefused.RANGE, "Заметка — не длиннее 200 знаков");
    }
    return value.trim();
  }
}
