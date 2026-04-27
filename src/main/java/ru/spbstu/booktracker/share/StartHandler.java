package ru.spbstu.booktracker.share;

import java.util.ArrayList;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.search.BookMetadata;
import ru.spbstu.booktracker.search.BookSearchService;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;
import ru.spbstu.booktracker.user.User;
import ru.spbstu.booktracker.user.UserService;

/**
 * {@code /start [payload]} — entry point. When the deep-link payload is {@code list_<chatId>}, the
 * recipient sees a read-only paginated view of the owner's reading list.
 */
@Component
public class StartHandler implements CommandHandler {

  private static final String SHARE_PREFIX = "list_";
  private static final String WELCOME =
      "Привет! Я Book Tracker and Advisor Bot. Используйте /help, чтобы увидеть список команд.";

  private final UserService userService;
  private final BookSearchService searchService;
  private final TelegramClient telegram;

  public StartHandler(
      UserService userService, BookSearchService searchService, TelegramClient telegram) {
    this.userService = userService;
    this.searchService = searchService;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "start";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    if (argument == null || argument.isBlank() || !argument.trim().startsWith(SHARE_PREFIX)) {
      telegram.sendMessage(chatId, WELCOME);
      return;
    }
    String ownerChatId = argument.trim().substring(SHARE_PREFIX.length());
    User owner;
    try {
      owner = userService.findOrCreate(ownerChatId);
    } catch (RuntimeException ex) {
      telegram.sendMessage(chatId, "Не удалось найти список этого пользователя.");
      return;
    }
    var items = new ArrayList<BookMetadata>();
    for (var it : owner.getReadingList()) {
      items.add(
          new BookMetadata(
              it.getBookId(), it.getTitle(), it.getAuthor(), it.getYear(), it.getDescription()));
    }
    if (items.isEmpty()) {
      telegram.sendMessage(chatId, "Список этого пользователя пока пуст.");
      return;
    }
    searchService.sendStaticList(chatId, "Список пользователя", items, ownerChatId);
  }
}
