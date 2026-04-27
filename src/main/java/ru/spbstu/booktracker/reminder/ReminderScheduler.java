package ru.spbstu.booktracker.reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.config.EnvProperties;
import ru.spbstu.booktracker.user.UserService;

/**
 * Scans MongoDB four times a minute (every 15 seconds) for users whose reminder time matches the
 * current {@code HH:mm} and pushes a {@link ReminderTask} into the ZeroMQ queue. A daily-reset
 * dedupe set guarantees each {@code chatId+HH:mm} pair fires at most once per day, so a slightly
 * delayed tick still delivers the reminder while subsequent ticks within the same minute are
 * skipped.
 */
@Component
public class ReminderScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
  private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
  private static final String REMINDER_TEXT =
      "📚 Напоминание: вы планировали почитать сегодня. Откройте /list reading и продолжите!";

  private final UserService userService;
  private final ZeroMqReminderQueue queue;
  private final EnvProperties env;
  private final Set<String> sentToday = ConcurrentHashMap.newKeySet();
  private volatile LocalDate dedupeDay;

  public ReminderScheduler(UserService userService, ZeroMqReminderQueue queue, EnvProperties env) {
    this.userService = userService;
    this.queue = queue;
    this.env = env;
  }

  /**
   * Runs every 15 seconds: at second 0, 15, 30 and 45 of every minute. The high frequency makes the
   * scheduler resilient against a single delayed tick (e.g. a long GC pause) while the day-keyed
   * dedupe set ensures each user is notified at most once per minute they configured.
   */
  @Scheduled(cron = "0/15 * * * * *")
  public void tick() {
    ZoneId zone = resolveZone();
    LocalDateTime now = LocalDateTime.now(zone);
    LocalDate today = now.toLocalDate();
    if (!today.equals(dedupeDay)) {
      sentToday.clear();
      dedupeDay = today;
    }
    String hhmm = now.format(HHMM);
    var users = userService.findUsersWithReminderAt(hhmm);
    if (users.isEmpty()) {
      return;
    }
    int dispatched = 0;
    for (var u : users) {
      String key = u.getChatId() + "@" + hhmm;
      if (!sentToday.add(key)) {
        continue;
      }
      try {
        queue.publish(new ReminderTask(Long.parseLong(u.getChatId()), REMINDER_TEXT));
        dispatched++;
      } catch (NumberFormatException ex) {
        log.warn("User has non-numeric chatId: {}", u.getChatId());
      }
    }
    if (dispatched > 0) {
      log.info("Pushed {} reminder(s) for {}", dispatched, hhmm);
    }
  }

  private ZoneId resolveZone() {
    String tz = env.getOrDefault("REMINDER_TZ", "Europe/Moscow");
    try {
      return ZoneId.of(tz);
    } catch (RuntimeException ex) {
      log.warn("Invalid REMINDER_TZ='{}', falling back to Europe/Moscow", tz);
      return ZoneId.of("Europe/Moscow");
    }
  }
}
