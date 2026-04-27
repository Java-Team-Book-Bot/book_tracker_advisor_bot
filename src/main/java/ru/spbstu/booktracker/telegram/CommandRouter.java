package ru.spbstu.booktracker.telegram;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** Dispatches a parsed command to the matching {@link CommandHandler}. */
@Component
public class CommandRouter {

  private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

  private final Map<String, CommandHandler> byCommand = new HashMap<>();
  private final TelegramClient telegram;

  public CommandRouter(List<CommandHandler> handlers, TelegramClient telegram) {
    this.telegram = telegram;
    for (CommandHandler h : handlers) {
      byCommand.put(h.command().toLowerCase(Locale.ROOT), h);
    }
    log.info("Registered {} command handlers: {}", byCommand.size(), byCommand.keySet());
  }

  /** Returns true if dispatched. */
  public boolean dispatch(Message message) {
    String text = message.getText();
    if (text == null || !text.startsWith("/")) {
      return false;
    }
    String trimmed = text.substring(1);
    int spaceIdx = trimmed.indexOf(' ');
    String cmd = (spaceIdx < 0 ? trimmed : trimmed.substring(0, spaceIdx));
    // Strip @BotName suffix if present.
    int atIdx = cmd.indexOf('@');
    if (atIdx >= 0) {
      cmd = cmd.substring(0, atIdx);
    }
    cmd = cmd.toLowerCase(Locale.ROOT);
    String arg = spaceIdx < 0 ? "" : trimmed.substring(spaceIdx + 1).trim();
    CommandHandler handler = byCommand.get(cmd);
    if (handler == null) {
      telegram.sendMessage(
          message.getChat().getId(), "Неизвестная команда. Введите /help для списка команд.");
      return true;
    }
    try {
      handler.handle(message, arg);
    } catch (RuntimeException ex) {
      log.error("Command handler '{}' failed", cmd, ex);
      telegram.sendMessage(
          message.getChat().getId(), "Произошла ошибка при выполнении команды. Попробуйте позже.");
    }
    return true;
  }
}
