package ru.spbstu.booktracker.reminder;

import org.springframework.stereotype.Service;
import ru.spbstu.booktracker.user.Reminder;
import ru.spbstu.booktracker.user.UserService;

/** Persists the {@link Reminder} settings for a user. */
@Service
public class ReminderConfigService {

  private final UserService userService;

  public ReminderConfigService(UserService userService) {
    this.userService = userService;
  }

  public void enable(String chatId, String hhmm) {
    var user = userService.findOrCreate(chatId);
    user.setReminder(new Reminder(true, hhmm));
    userService.save(user);
  }

  public void disable(String chatId) {
    var user = userService.findOrCreate(chatId);
    user.setReminder(new Reminder(false, user.getReminder().getTime()));
    userService.save(user);
  }
}
