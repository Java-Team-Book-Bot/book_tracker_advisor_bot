package ru.spbstu.booktracker.reminder;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /remind on <HH:mm> | /remind off}. */
@Component
public class RemindHandler implements CommandHandler {

  private final ReminderConfigService config;
  private final TelegramClient telegram;

  public RemindHandler(ReminderConfigService config, TelegramClient telegram) {
    this.config = config;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "remind";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String[] parts = argument == null ? new String[0] : argument.trim().split("\\s+");
    if (parts.length == 0 || parts[0].isBlank()) {
      telegram.sendMessage(
          chatId,
          "Используйте: <code>/remind on &lt;ЧЧ:ММ&gt;</code> или <code>/remind off</code>.");
      return;
    }
    String mode = parts[0].toLowerCase(java.util.Locale.ROOT);
    switch (mode) {
      case "on" -> {
        if (parts.length < 2 || !Validation.isValidHhmm(parts[1])) {
          telegram.sendMessage(
              chatId, "Укажите время в формате ЧЧ:ММ. Пример: <code>/remind on 21:00</code>.");
          return;
        }
        config.enable(String.valueOf(chatId), parts[1]);
        telegram.sendMessage(chatId, "Ежедневные напоминания включены на " + parts[1] + ".");
      }
      case "off" -> {
        config.disable(String.valueOf(chatId));
        telegram.sendMessage(chatId, "Напоминания отключены.");
      }
      default ->
          telegram.sendMessage(
              chatId,
              "Используйте: <code>/remind on &lt;ЧЧ:ММ&gt;</code> или <code>/remind off</code>.");
    }
  }
}
