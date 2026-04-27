package ru.spbstu.booktracker.tracker;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /remove <n>} (Requirements §3.3). */
@Component
public class RemoveHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public RemoveHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "remove";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    var idxOpt = Validation.parseIndex(argument == null ? "" : argument.trim());
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер из списка. Пример: /remove 3.");
      return;
    }
    var removed = tracker.remove(String.valueOf(chatId), idxOpt.get());
    if (removed.isEmpty()) {
      telegram.sendMessage(
          chatId, "Книга с таким номером не найдена. Проверьте список через команду /list.");
      return;
    }
    telegram.sendMessage(
        chatId,
        "Книга «"
            + TextUtils.escapeHtml(removed.get().getTitle())
            + "» успешно удалена из вашего списка.");
  }
}
