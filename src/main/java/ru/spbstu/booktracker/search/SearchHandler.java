package ru.spbstu.booktracker.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.preferences.PreferencesService;
import ru.spbstu.booktracker.telegram.InProgressRegistry;
import ru.spbstu.booktracker.telegram.InProgressRegistry.Kind;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /search} command (Requirements §3.4). */
@Component
public class SearchHandler implements CommandHandler {

  private static final int MAX_QUERY_LENGTH = 100;

  private final BookSearchService search;
  private final PreferencesService preferences;
  private final TelegramClient telegram;
  private final InProgressRegistry inProgress;

  public SearchHandler(
      BookSearchService search,
      PreferencesService preferences,
      TelegramClient telegram,
      InProgressRegistry inProgress) {
    this.search = search;
    this.preferences = preferences;
    this.telegram = telegram;
    this.inProgress = inProgress;
  }

  @Override
  public String command() {
    return "search";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String query = argument == null ? "" : argument.trim();
    if (query.length() > MAX_QUERY_LENGTH) {
      telegram.sendMessage(
          chatId,
          "Ваш запрос длиннее 100 символов. Пожалуйста, сократите его и повторите попытку.");
      return;
    }
    List<String> candidates;
    if (query.isEmpty()) {
      List<String> prefs = preferences.list(String.valueOf(chatId));
      if (prefs.isEmpty()) {
        telegram.sendMessage(
            chatId,
            "Критерии поиска не заданы, а ваш список предпочтений пуст. Уточните запрос (например,"
                + " <code>/search Дюна</code>) или добавьте любимые жанры через"
                + " <code>/preferences add &lt;текст&gt;</code>.");
        return;
      }
      candidates = buildPreferenceCandidates(prefs);
    } else {
      candidates = List.of(query);
    }
    if (!inProgress.tryAcquire(chatId, Kind.SEARCH)) {
      long warn =
          telegram.sendMessage(
              chatId, "Ваш предыдущий запрос ещё обрабатывается, пожалуйста, подождите.");
      scheduleAutoDelete(chatId, warn);
      return;
    }
    try {
      search.sendFirstPage(chatId, candidates);
    } finally {
      inProgress.release(chatId, Kind.SEARCH);
    }
  }

  /**
   * Builds a list of fallback queries from the user's preferences. The first candidate combines
   * preferences via Google Books' {@code OR} operator (best diversity); subsequent candidates are
   * individual preferences in random order, so a result is returned even when no book matches the
   * union.
   */
  private List<String> buildPreferenceCandidates(List<String> prefs) {
    var shuffled = new ArrayList<>(prefs);
    Collections.shuffle(shuffled);
    var candidates = new ArrayList<String>();
    var or = new StringBuilder();
    for (String pref : shuffled) {
      String trimmed = pref.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String token = "\"" + trimmed.replace("\"", "") + "\"";
      String next = or.length() == 0 ? token : or + " OR " + token;
      if (next.length() <= MAX_QUERY_LENGTH) {
        or.setLength(0);
        or.append(next);
      }
    }
    if (or.length() > 0) {
      candidates.add(or.toString());
    }
    for (String pref : shuffled) {
      String trimmed = pref.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.length() > MAX_QUERY_LENGTH) {
        trimmed = trimmed.substring(0, MAX_QUERY_LENGTH);
      }
      if (!candidates.contains(trimmed)) {
        candidates.add(trimmed);
      }
    }
    return candidates;
  }

  private void scheduleAutoDelete(long chatId, long messageId) {
    if (messageId <= 0) {
      return;
    }
    var t =
        new Thread(
            () -> {
              try {
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
              }
              telegram.deleteMessage(chatId, messageId);
            },
            "auto-delete-" + messageId);
    t.setDaemon(true);
    t.start();
  }
}
