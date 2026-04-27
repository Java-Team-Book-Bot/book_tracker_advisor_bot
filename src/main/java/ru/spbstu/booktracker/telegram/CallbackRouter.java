package ru.spbstu.booktracker.telegram;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.dto.CallbackQuery;
import ru.spbstu.booktracker.telegram.handlers.CallbackHandler;

/** Routes a {@link CallbackQuery} to the handler whose prefix matches its data. */
@Component
public class CallbackRouter {

  private static final Logger log = LoggerFactory.getLogger(CallbackRouter.class);

  private final List<CallbackHandler> handlers;
  private final TelegramClient telegram;

  public CallbackRouter(List<CallbackHandler> handlers, TelegramClient telegram) {
    // Longer prefixes win over shorter ones (e.g. "book:summary:" before "book:").
    this.handlers =
        handlers.stream()
            .sorted(Comparator.comparingInt((CallbackHandler h) -> h.prefix().length()).reversed())
            .toList();
    this.telegram = telegram;
    log.info(
        "Registered {} callback handlers: {}",
        handlers.size(),
        handlers.stream().map(CallbackHandler::prefix).toList());
  }

  public void dispatch(CallbackQuery callback) {
    String data = callback.getData();
    if (data == null) {
      telegram.answerCallbackQuery(callback.getId());
      return;
    }
    for (CallbackHandler h : handlers) {
      if (data.startsWith(h.prefix())) {
        try {
          h.handle(callback);
        } catch (RuntimeException ex) {
          log.error("Callback handler for prefix '{}' failed", h.prefix(), ex);
          telegram.answerCallbackQuery(callback.getId(), "Ошибка");
        }
        return;
      }
    }
    log.debug("No handler matched callback data '{}'", data);
    telegram.answerCallbackQuery(callback.getId());
  }
}
