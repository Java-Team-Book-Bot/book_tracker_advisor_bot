package ru.spbstu.booktracker.tracker;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /rate <n> <1-10>} (Requirements §3.6). */
@Component
public class RateHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public RateHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "rate";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String[] parts = argument == null ? new String[0] : argument.trim().split("\\s+", 2);
    if (parts.length < 2) {
      telegram.sendMessage(chatId, "Используйте: <code>/rate &lt;номер&gt; &lt;1-10&gt;</code>.");
      return;
    }
    var idxOpt = Validation.parseIndex(parts[0]);
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер из списка.");
      return;
    }
    int rating;
    try {
      rating = Integer.parseInt(parts[1]);
    } catch (NumberFormatException ex) {
      telegram.sendMessage(chatId, "Оценка должна быть числом от 1 до 10.");
      return;
    }
    var result = tracker.rate(String.valueOf(chatId), idxOpt.get(), rating);
    switch (result.outcome()) {
      case OK ->
          telegram.sendMessage(
              chatId,
              "Оценка "
                  + rating
                  + "/10 сохранена для книги «"
                  + TextUtils.escapeHtml(result.item().getTitle())
                  + "».");
      case NOT_FOUND -> telegram.sendMessage(chatId, "Книга с таким ID не найдена в вашем списке.");
      case WRONG_STATUS ->
          telegram.sendMessage(chatId, "Оценивать можно только книги со статусом «Прочитано».");
      case INVALID -> telegram.sendMessage(chatId, "Оценка должна быть числом от 1 до 10.");
      default -> {
        // exhaustive
      }
    }
  }
}
