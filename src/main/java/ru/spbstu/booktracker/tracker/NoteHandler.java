package ru.spbstu.booktracker.tracker;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /note <n> <text>} (Requirements §3.6). */
@Component
public class NoteHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public NoteHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "note";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String[] parts = argument == null ? new String[0] : argument.trim().split("\\s+", 2);
    if (parts.length < 2) {
      telegram.sendMessage(chatId, "Используйте: <code>/note &lt;номер&gt; &lt;текст&gt;</code>.");
      return;
    }
    var idxOpt = Validation.parseIndex(parts[0]);
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер из списка.");
      return;
    }
    String text = parts[1].trim();
    var result = tracker.note(String.valueOf(chatId), idxOpt.get(), text);
    switch (result.outcome()) {
      case OK ->
          telegram.sendMessage(
              chatId,
              "Заметка для книги «"
                  + TextUtils.escapeHtml(result.item().getTitle())
                  + "» сохранена.");
      case NOT_FOUND -> telegram.sendMessage(chatId, "Книга с таким ID не найдена в вашем списке.");
      case WRONG_STATUS ->
          telegram.sendMessage(
              chatId, "Заметку можно добавлять только для книг со статусом «Прочитано».");
      case INVALID -> telegram.sendMessage(chatId, "Длина заметки должна быть до 300 символов.");
      default -> {
        // exhaustive
      }
    }
  }
}
