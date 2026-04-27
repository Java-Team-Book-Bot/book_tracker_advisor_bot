package ru.spbstu.booktracker.search;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /random}: pick one random book from Google Books and send a card. */
@Component
public class RandomHandler implements CommandHandler {

  private static final Logger log = LoggerFactory.getLogger(RandomHandler.class);
  private static final List<String> SEED_QUERIES =
      List.of(
          "subject:fiction",
          "subject:fantasy",
          "subject:mystery",
          "subject:science_fiction",
          "subject:history",
          "subject:biography",
          "subject:adventure",
          "subject:poetry",
          "subject:philosophy");

  private final BookSearchService search;
  private final BookCardRenderer renderer;
  private final TelegramClient telegram;

  public RandomHandler(
      BookSearchService search, BookCardRenderer renderer, TelegramClient telegram) {
    this.search = search;
    this.renderer = renderer;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "random";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String seed = SEED_QUERIES.get(ThreadLocalRandom.current().nextInt(SEED_QUERIES.size()));
    int startIndex = ThreadLocalRandom.current().nextInt(40);
    try {
      var page = search.searchOnce(seed, startIndex);
      if (page.isEmpty()) {
        telegram.sendMessage(chatId, "Не удалось подобрать случайную книгу. Попробуйте ещё раз.");
        return;
      }
      var pick = page.get(ThreadLocalRandom.current().nextInt(page.size()));
      telegram.sendMessage(
          chatId, renderer.render(pick), renderer.randomCardKeyboard(pick.bookId()));
    } catch (RuntimeException ex) {
      log.warn("/random failed: {}", ex.getMessage());
      telegram.sendMessage(chatId, "Не удалось подобрать случайную книгу. Попробуйте ещё раз.");
    }
  }
}
