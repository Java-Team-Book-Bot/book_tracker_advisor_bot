package ru.spbstu.booktracker.search;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.telegram.dto.InlineKeyboard;
import ru.spbstu.booktracker.telegram.dto.InlineKeyboard.Button;

/** Builds the detail card text + inline keyboard for a single book. */
@Component
public class BookCardRenderer {

  public String render(BookMetadata book) {
    var sb = new StringBuilder();
    sb.append("<b>").append(TextUtils.escapeHtml(book.title())).append("</b>\n");
    if (book.author() != null && !book.author().isBlank()) {
      sb.append("<i>").append(TextUtils.escapeHtml(book.author())).append("</i>\n");
    }
    if (book.year() != null && !book.year().isBlank()) {
      sb.append("Год: ").append(TextUtils.escapeHtml(book.year())).append('\n');
    }
    sb.append('\n');
    String desc = book.description();
    if (desc != null && !desc.isBlank()) {
      sb.append(TextUtils.escapeHtml(TextUtils.truncate(desc, 1500)));
    } else {
      sb.append("<i>Описание отсутствует.</i>");
    }
    return sb.toString();
  }

  /** Buttons under detail card from a search-list session. */
  public InlineKeyboard searchCardKeyboard(String bookId) {
    return InlineKeyboard.create()
        .row(
            Button.of("Добавить в список", "book:add:" + bookId),
            Button.of("\uD83E\uDD16 Саммари от LLM", "book:summary:" + bookId))
        .row(Button.of("« Назад к списку", "srch:back"));
  }

  /** Buttons under a /random card (no list to go back to). */
  public InlineKeyboard randomCardKeyboard(String bookId) {
    return InlineKeyboard.create()
        .row(
            Button.of("Добавить в список", "book:add:" + bookId),
            Button.of("\uD83E\uDD16 Саммари от LLM", "book:summary:" + bookId));
  }

  /** Buttons under a card from a shared (read-only) reading list. */
  public InlineKeyboard sharedCardKeyboard(String bookId) {
    return InlineKeyboard.create()
        .row(Button.of("Добавить в свой список", "book:add:" + bookId))
        .row(Button.of("« Назад к списку", "srch:back"));
  }

  /** Status-selection sub-keyboard. */
  public InlineKeyboard statusKeyboard(String bookId) {
    return InlineKeyboard.create()
        .row(
            Button.of("Хочу прочитать", "book:status:" + bookId + ":to_read"),
            Button.of("Читаю", "book:status:" + bookId + ":reading"),
            Button.of("Прочитано", "book:status:" + bookId + ":finished"));
  }
}
