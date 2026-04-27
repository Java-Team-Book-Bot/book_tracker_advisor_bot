package ru.spbstu.booktracker.http;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.spbstu.booktracker.user.User;
import ru.spbstu.booktracker.user.UserService;

/** Admin-only {@code GET /users} endpoint. */
@RestController
public class AdminUsersController {

  private final UserService userService;

  public AdminUsersController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/users")
  public List<UserView> listUsers() {
    return userService.findAll().stream().map(UserView::from).toList();
  }

  /** Compact projection for the admin endpoint. */
  public record UserView(
      String id,
      String chatId,
      int readingListSize,
      int preferencesCount,
      boolean reminderEnabled) {

    public static UserView from(User u) {
      return new UserView(
          u.getId(),
          u.getChatId(),
          u.getReadingList().size(),
          u.getPreferences().size(),
          u.getReminder() != null && u.getReminder().isEnabled());
    }
  }
}
