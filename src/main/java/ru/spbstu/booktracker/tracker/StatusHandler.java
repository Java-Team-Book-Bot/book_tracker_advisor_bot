package ru.spbstu.booktracker.tracker;

import java.util.Locale;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;
import ru.spbstu.booktracker.user.BookStatus;

/** {@code /status <номер> <new>} (Requirements §3.3). */
@Component
public class StatusHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public StatusHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "status";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String[] parts = argument == null ? new String[0] : argument.trim().split("\\s+", 2);
    if (parts.length < 2) {
      telegram.sendMessage(
          chatId,
          "Используйте: <code>/status &lt;номер&gt; &lt;статус&gt;</code>.\nСтатусы: хочу / читаю / прочитано.");
      return;
    }
    var idxOpt = Validation.parseIndex(parts[0]);
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер из списка. Пример: /status 1 читаю.");
      return;
    }
    var statusOpt = BookStatus.fromCode(parts[1].toLowerCase(Locale.ROOT));
    if (statusOpt.isEmpty()) {
      telegram.sendMessage(
          chatId, "Укажите корректный статус. Доступные варианты: хочу / читаю / прочитано.");
      return;
    }
    var item = tracker.findByIndex(String.valueOf(chatId), idxOpt.get());
    if (item.isEmpty()) {
      telegram.sendMessage(
          chatId,
          "Книга с ID "
              + idxOpt.get()
              + " не найдена в вашем списке. Проверьте ID через команду /list.");
      return;
    }
    boolean ok = tracker.changeStatus(String.valueOf(chatId), idxOpt.get(), statusOpt.get());
    if (ok) {
      telegram.sendMessage(
          chatId,
          "Статус книги «"
              + TextUtils.escapeHtml(item.get().getTitle())
              + "» успешно изменён на «"
              + statusOpt.get().label()
              + "».");
    }
  }
}
