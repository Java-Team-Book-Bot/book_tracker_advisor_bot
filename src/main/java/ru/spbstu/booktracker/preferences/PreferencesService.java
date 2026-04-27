package ru.spbstu.booktracker.preferences;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.spbstu.booktracker.user.User;
import ru.spbstu.booktracker.user.UserService;

/** Business logic for the user's literary preferences (Requirements §3.2). */
@Service
public class PreferencesService {

  public static final int LIMIT = 15;
  public static final int MAX_LENGTH = 50;

  private final UserService userService;

  public PreferencesService(UserService userService) {
    this.userService = userService;
  }

  public AddResult add(String chatId, String text) {
    if (text == null || text.isBlank()) {
      return AddResult.MISSING_TEXT;
    }
    if (text.length() > MAX_LENGTH) {
      return AddResult.TOO_LONG;
    }
    User user = userService.findOrCreate(chatId);
    if (user.getPreferences().size() >= LIMIT) {
      return AddResult.LIMIT_REACHED;
    }
    user.getPreferences().add(text);
    userService.save(user);
    return AddResult.OK;
  }

  public List<String> list(String chatId) {
    return List.copyOf(userService.findOrCreate(chatId).getPreferences());
  }

  public RemoveResult remove(String chatId, int oneBasedIndex) {
    User user = userService.findOrCreate(chatId);
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= user.getPreferences().size()) {
      return new RemoveResult(false, null);
    }
    String removed = user.getPreferences().remove(idx);
    userService.save(user);
    return new RemoveResult(true, removed);
  }

  /** Outcome of {@link #add}. */
  public enum AddResult {
    OK,
    MISSING_TEXT,
    TOO_LONG,
    LIMIT_REACHED
  }

  /** Outcome of {@link #remove}. */
  public record RemoveResult(boolean removed, String value) {}
}
