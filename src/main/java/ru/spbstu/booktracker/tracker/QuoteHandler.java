package ru.spbstu.booktracker.tracker;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /quote add <n> <text>} (Requirements §3.6). */
@Component
public class QuoteHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public QuoteHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "quote";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String[] parts = argument == null ? new String[0] : argument.trim().split("\\s+", 3);
    if (parts.length < 3 || !"add".equalsIgnoreCase(parts[0])) {
      telegram.sendMessage(
          chatId, "Используйте: <code>/quote add &lt;номер&gt; &lt;текст цитаты&gt;</code>.");
      return;
    }
    var idxOpt = Validation.parseIndex(parts[1]);
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер из списка.");
      return;
    }
    String quote = parts[2].trim();
    var result = tracker.addQuote(String.valueOf(chatId), idxOpt.get(), quote);
    switch (result.outcome()) {
      case OK ->
          telegram.sendMessage(
              chatId,
              "Цитата сохранена для книги «"
                  + TextUtils.escapeHtml(result.item().getTitle())
                  + "».");
      case NOT_FOUND -> telegram.sendMessage(chatId, "Книга с таким ID не найдена в вашем списке.");
      case LIMIT_REACHED ->
          telegram.sendMessage(
              chatId,
              "Достигнут лимит цитат (10). Удалите неактуальные цитаты, чтобы добавить новую.");
      case TOO_LONG ->
          telegram.sendMessage(chatId, "Длина цитаты должна быть от 1 до 500 символов.");
      default -> {
        // exhaustive
      }
    }
  }
}
