package ru.spbstu.booktracker.tracker;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /progress <n> <value>} (Requirements §3.3). */
@Component
public class ProgressHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public ProgressHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "progress";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String[] parts = argument == null ? new String[0] : argument.trim().split("\\s+", 2);
    if (parts.length < 2) {
      telegram.sendMessage(
          chatId, "Используйте: <code>/progress &lt;номер&gt; &lt;значение&gt;</code>.");
      return;
    }
    var idxOpt = Validation.parseIndex(parts[0]);
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер из списка.");
      return;
    }
    String value = parts[1].trim();
    if (value.length() > TrackerService.PROGRESS_MAX) {
      telegram.sendMessage(
          chatId, "Ваш запрос длиннее 50 символов. Пожалуйста, сократите его и повторите попытку.");
      return;
    }
    if (!Validation.isValidProgress(value)) {
      telegram.sendMessage(
          chatId,
          "Значение прогресса должно содержать только буквы, цифры и пробелы (до 50 символов).");
      return;
    }
    var result = tracker.updateProgress(String.valueOf(chatId), idxOpt.get(), value);
    switch (result.outcome()) {
      case OK ->
          telegram.sendMessage(
              chatId,
              "Прогресс для книги «"
                  + TextUtils.escapeHtml(result.item().getTitle())
                  + "» обновлён: "
                  + TextUtils.escapeHtml(value)
                  + ".");
      case NOT_FOUND -> telegram.sendMessage(chatId, "Книга с таким ID не найдена в вашем списке.");
      case WRONG_STATUS ->
          telegram.sendMessage(
              chatId,
              "Обновлять прогресс можно только для книг в статусе «Читаю». Текущий статус этой книги: «"
                  + result.item().getStatus().label()
                  + "».");
      default -> {
        // exhaustive
      }
    }
  }
}
