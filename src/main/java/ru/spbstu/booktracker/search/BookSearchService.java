package ru.spbstu.booktracker.search;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.InlineKeyboard;
import ru.spbstu.booktracker.telegram.dto.InlineKeyboard.Button;

/**
 * Renders search-result list messages with inline pagination/selection buttons (Requirements §3.4).
 */
@Service
public class BookSearchService {

  private static final Logger log = LoggerFactory.getLogger(BookSearchService.class);

  private final GoogleBooksClient googleBooks;
  private final TelegramClient telegram;
  private final SearchSessionStore sessions;

  public BookSearchService(
      GoogleBooksClient googleBooks, TelegramClient telegram, SearchSessionStore sessions) {
    this.googleBooks = googleBooks;
    this.telegram = telegram;
    this.sessions = sessions;
  }

  /** Sends an initial result page; returns {@code true} on success. */
  public boolean sendFirstPage(long chatId, String query) {
    return sendFirstPage(chatId, List.of(query));
  }

  /**
   * Tries each candidate query in order and renders the first non-empty page. Returns {@code true}
   * on success.
   */
  public boolean sendFirstPage(long chatId, List<String> queryCandidates) {
    try {
      String winningQuery = null;
      List<BookMetadata> page = List.of();
      for (String candidate : queryCandidates) {
        if (candidate == null || candidate.isBlank()) {
          continue;
        }
        var attempt = googleBooks.search(candidate, 0);
        if (!attempt.isEmpty()) {
          winningQuery = candidate;
          page = attempt;
          break;
        }
      }
      if (page.isEmpty() || winningQuery == null) {
        telegram.sendMessage(chatId, "По запросу ничего не найдено. Уточните запрос.");
        return false;
      }
      var session = new SearchSession(winningQuery);
      session.setStartIndex(0);
      session.setPage(page);
      String text = renderListText(session);
      var keyboard = renderListKeyboard(session);
      long msgId = telegram.sendMessage(chatId, text, keyboard);
      if (msgId > 0) {
        sessions.put(chatId, msgId, session);
      }
      return true;
    } catch (RuntimeException ex) {
      log.warn("Search failed: {}", ex.getMessage());
      telegram.sendMessage(
          chatId, "Не удалось выполнить поиск через Google Books. Попробуйте позже.");
      return false;
    }
  }

  /** Re-renders the list view at {@code newStartIndex} (used by Next/Prev buttons). */
  public void showListPage(long chatId, long messageId, SearchSession session, int newStartIndex) {
    int normalised = Math.max(0, newStartIndex);
    try {
      List<BookMetadata> page;
      if (session.getStaticItems() != null) {
        var all = session.getStaticItems();
        if (normalised >= all.size()) {
          return;
        }
        int end = Math.min(all.size(), normalised + GoogleBooksClient.PAGE_SIZE);
        page = all.subList(normalised, end);
      } else {
        page = googleBooks.search(session.getQuery(), normalised);
        if (page.isEmpty()) {
          return;
        }
      }
      session.setStartIndex(normalised);
      session.setPage(page);
      sessions.put(chatId, messageId, session);
      telegram.editMessageText(
          chatId, messageId, renderListText(session), renderListKeyboard(session));
    } catch (RuntimeException ex) {
      log.warn("showListPage failed: {}", ex.getMessage());
    }
  }

  /** Sends a read-only paginated view of a pre-built list of books. */
  public boolean sendStaticList(
      long chatId, String headline, List<BookMetadata> all, String ownerChatId) {
    if (all.isEmpty()) {
      telegram.sendMessage(chatId, "Список пуст.");
      return false;
    }
    var session = new SearchSession(headline, true, ownerChatId);
    session.setStaticItems(all);
    session.setStartIndex(0);
    session.setPage(all.subList(0, Math.min(GoogleBooksClient.PAGE_SIZE, all.size())));
    long msgId = telegram.sendMessage(chatId, renderListText(session), renderListKeyboard(session));
    if (msgId > 0) {
      sessions.put(chatId, msgId, session);
    }
    return msgId > 0;
  }

  public String renderListText(SearchSession session) {
    var sb = new StringBuilder();
    sb.append("<b>Результаты поиска</b>");
    if (session.getQuery() != null && !session.getQuery().isBlank()) {
      String displayQuery = session.getQuery().replace("\" OR \"", "\" или \"");
      sb.append(" — ").append(TextUtils.escapeHtml(displayQuery));
    }
    sb.append('\n').append('\n');
    var page = session.getPage();
    for (int i = 0; i < page.size(); i++) {
      sb.append(i + 1)
          .append(". ")
          .append(TextUtils.escapeHtml(page.get(i).shortLine()))
          .append('\n');
    }
    return sb.toString();
  }

  public InlineKeyboard renderListKeyboard(SearchSession session) {
    var kb = InlineKeyboard.create();
    var page = session.getPage();
    var numberRow = new java.util.ArrayList<Button>();
    for (int i = 0; i < page.size(); i++) {
      numberRow.add(Button.of("[" + (i + 1) + "]", "srch:pick:" + i));
    }
    kb.row(numberRow.toArray(Button[]::new));
    var navRow = new java.util.ArrayList<Button>();
    if (session.getStartIndex() >= GoogleBooksClient.PAGE_SIZE) {
      navRow.add(Button.of("« Назад", "srch:nav:prev"));
    }
    boolean hasNext;
    if (session.getStaticItems() != null) {
      hasNext = session.getStartIndex() + page.size() < session.getStaticItems().size();
    } else {
      hasNext = page.size() == GoogleBooksClient.PAGE_SIZE;
    }
    if (hasNext) {
      navRow.add(Button.of("Далее »", "srch:nav:next"));
    }
    if (!navRow.isEmpty()) {
      kb.row(navRow.toArray(Button[]::new));
    }
    return kb;
  }

  public List<BookMetadata> searchOnce(String query, int startIndex) {
    return googleBooks.search(query, startIndex);
  }
}
