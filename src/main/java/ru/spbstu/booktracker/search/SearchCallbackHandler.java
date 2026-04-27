package ru.spbstu.booktracker.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.CallbackQuery;
import ru.spbstu.booktracker.telegram.handlers.CallbackHandler;

/** Handles callbacks with prefix {@code srch:} (search list/card navigation). */
@Component
public class SearchCallbackHandler implements CallbackHandler {

  private static final Logger log = LoggerFactory.getLogger(SearchCallbackHandler.class);

  private final BookSearchService searchService;
  private final BookCardRenderer renderer;
  private final SearchSessionStore sessions;
  private final TelegramClient telegram;

  public SearchCallbackHandler(
      BookSearchService searchService,
      BookCardRenderer renderer,
      SearchSessionStore sessions,
      TelegramClient telegram) {
    this.searchService = searchService;
    this.renderer = renderer;
    this.sessions = sessions;
    this.telegram = telegram;
  }

  @Override
  public String prefix() {
    return "srch:";
  }

  @Override
  public void handle(CallbackQuery callback) {
    long chatId = callback.getMessage().getChat().getId();
    long messageId = callback.getMessage().getMessageId();
    String data = callback.getData();
    var session = sessions.get(chatId, messageId);
    if (session == null) {
      telegram.answerCallbackQuery(callback.getId(), "Сессия поиска устарела. Повторите /search.");
      return;
    }
    String[] parts = data.split(":");
    String action = parts.length > 1 ? parts[1] : "";
    switch (action) {
      case "nav" -> handleNav(chatId, messageId, session, parts);
      case "pick" -> handlePick(chatId, messageId, session, parts, callback);
      case "back" -> handleBack(chatId, messageId, session);
      default -> log.debug("Unknown srch action '{}'", action);
    }
    telegram.answerCallbackQuery(callback.getId());
  }

  private void handleNav(long chatId, long messageId, SearchSession session, String[] parts) {
    if (parts.length < 3) {
      return;
    }
    int delta =
        "next".equals(parts[2]) ? GoogleBooksClient.PAGE_SIZE : -GoogleBooksClient.PAGE_SIZE;
    int newStart = Math.max(0, session.getStartIndex() + delta);
    searchService.showListPage(chatId, messageId, session, newStart);
  }

  private void handlePick(
      long chatId, long messageId, SearchSession session, String[] parts, CallbackQuery callback) {
    if (parts.length < 3) {
      return;
    }
    int idx;
    try {
      idx = Integer.parseInt(parts[2]);
    } catch (NumberFormatException ex) {
      return;
    }
    if (idx < 0 || idx >= session.getPage().size()) {
      return;
    }
    var book = session.getPage().get(idx);
    var keyboard =
        session.isReadOnly()
            ? renderer.sharedCardKeyboard(book.bookId())
            : renderer.searchCardKeyboard(book.bookId());
    telegram.editMessageText(chatId, messageId, renderer.render(book), keyboard);
  }

  private void handleBack(long chatId, long messageId, SearchSession session) {
    telegram.editMessageText(
        chatId,
        messageId,
        searchService.renderListText(session),
        searchService.renderListKeyboard(session));
  }
}
