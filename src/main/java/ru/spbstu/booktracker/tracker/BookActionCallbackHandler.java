package ru.spbstu.booktracker.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.search.BookCardRenderer;
import ru.spbstu.booktracker.search.BookMetadata;
import ru.spbstu.booktracker.search.GoogleBooksClient;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.CallbackQuery;
import ru.spbstu.booktracker.telegram.handlers.CallbackHandler;
import ru.spbstu.booktracker.user.BookStatus;

/**
 * Handles {@code book:add:*} and {@code book:status:*} callbacks (adding a book to the reading list
 * with a chosen status). LLM summary callback is handled by the LLM module.
 */
@Component
public class BookActionCallbackHandler implements CallbackHandler {

  private static final Logger log = LoggerFactory.getLogger(BookActionCallbackHandler.class);

  private final TrackerService tracker;
  private final BookCardRenderer renderer;
  private final TelegramClient telegram;
  private final GoogleBooksClient googleBooks;

  public BookActionCallbackHandler(
      TrackerService tracker,
      BookCardRenderer renderer,
      TelegramClient telegram,
      GoogleBooksClient googleBooks) {
    this.tracker = tracker;
    this.renderer = renderer;
    this.telegram = telegram;
    this.googleBooks = googleBooks;
  }

  @Override
  public String prefix() {
    return "book:";
  }

  @Override
  public void handle(CallbackQuery callback) {
    String[] parts = callback.getData().split(":");
    if (parts.length < 3) {
      telegram.answerCallbackQuery(callback.getId());
      return;
    }
    String action = parts[1];
    String bookId = parts[2];
    long chatId = callback.getMessage().getChat().getId();
    long messageId = callback.getMessage().getMessageId();
    switch (action) {
      case "add" -> handleAdd(callback, chatId, messageId, bookId);
      case "status" -> {
        if (parts.length < 4) {
          telegram.answerCallbackQuery(callback.getId());
          return;
        }
        handleStatus(callback, chatId, messageId, bookId, parts[3]);
      }
      default -> {
        // summary handled elsewhere
      }
    }
  }

  private void handleAdd(CallbackQuery callback, long chatId, long messageId, String bookId) {
    var keyboard = renderer.statusKeyboard(bookId);
    telegram.editMessageText(
        chatId,
        messageId,
        callback.getMessage().getText() == null
            ? "Выберите статус книги:"
            : prependPrompt(callback),
        keyboard);
    telegram.answerCallbackQuery(callback.getId());
  }

  private String prependPrompt(CallbackQuery callback) {
    String prev = callback.getMessage().getText();
    return (prev == null ? "" : prev) + "\n\n<i>Выберите статус книги:</i>";
  }

  private void handleStatus(
      CallbackQuery callback, long chatId, long messageId, String bookId, String statusCode) {
    var statusOpt = BookStatus.fromCode(statusCode);
    if (statusOpt.isEmpty()) {
      telegram.answerCallbackQuery(callback.getId(), "Неверный статус");
      return;
    }
    BookStatus status = statusOpt.get();
    BookMetadata book;
    try {
      book = googleBooks.get(bookId);
    } catch (RuntimeException ex) {
      log.warn("Failed to fetch book {}: {}", bookId, ex.getMessage());
      telegram.answerCallbackQuery(callback.getId(), "Не удалось получить данные о книге.");
      return;
    }
    if (book == null) {
      telegram.answerCallbackQuery(callback.getId(), "Книга не найдена.");
      return;
    }
    var result = tracker.addBook(String.valueOf(chatId), book, status);
    switch (result.outcome()) {
      case ADDED ->
          telegram.editMessageText(
              chatId,
              messageId,
              (callback.getMessage().getText() == null ? "" : callback.getMessage().getText())
                  + "\n\n<b>Книга добавлена в список со статусом:</b> "
                  + status.label(),
              null);
      case ALREADY_PRESENT ->
          telegram.answerCallbackQuery(
              callback.getId(),
              "Эта книга уже есть в списке со статусом «" + result.existingStatus().label() + "».");
      case LIMIT_REACHED ->
          telegram.answerCallbackQuery(
              callback.getId(),
              "Достигнут лимит списка (200 книг). Удалите неактуальные записи через /remove <номер>.");
      default -> {
        // exhaustive
      }
    }
  }
}
