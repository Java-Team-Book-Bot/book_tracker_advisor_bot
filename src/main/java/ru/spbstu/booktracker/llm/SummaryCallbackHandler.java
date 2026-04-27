package ru.spbstu.booktracker.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.search.GoogleBooksClient;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.CallbackQuery;
import ru.spbstu.booktracker.telegram.handlers.CallbackHandler;

/**
 * Handles {@code book:summary:<bookId>} callback. Delegates concurrency control to {@link
 * LlmAdvisorService}: a duplicate click while another summary is generating is reported with the
 * required text.
 */
@Component
public class SummaryCallbackHandler implements CallbackHandler {

  private static final Logger log = LoggerFactory.getLogger(SummaryCallbackHandler.class);

  private final GoogleBooksClient googleBooks;
  private final PromptBuilder prompts;
  private final LlmAdvisorService llm;
  private final TelegramClient telegram;

  public SummaryCallbackHandler(
      GoogleBooksClient googleBooks,
      PromptBuilder prompts,
      LlmAdvisorService llm,
      TelegramClient telegram) {
    this.googleBooks = googleBooks;
    this.prompts = prompts;
    this.llm = llm;
    this.telegram = telegram;
  }

  @Override
  public String prefix() {
    return "book:summary:";
  }

  @Override
  public void handle(CallbackQuery callback) {
    String bookId = callback.getData().substring(prefix().length());
    long chatId = callback.getMessage().getChat().getId();
    try {
      var book = googleBooks.get(bookId);
      if (book == null) {
        telegram.answerCallbackQuery(callback.getId(), "Книга не найдена.");
        return;
      }
      boolean accepted = llm.submit(chatId, prompts.summary(book.title(), book.author()));
      telegram.answerCallbackQuery(
          callback.getId(), accepted ? null : "Саммари уже генерируется, пожалуйста, подождите.");
    } catch (RuntimeException ex) {
      log.warn("Summary callback failed: {}", ex.getMessage());
      telegram.answerCallbackQuery(callback.getId(), "Не удалось получить данные о книге.");
    }
  }
}
