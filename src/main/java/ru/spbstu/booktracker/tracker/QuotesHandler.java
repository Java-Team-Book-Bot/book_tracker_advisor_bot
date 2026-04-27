package ru.spbstu.booktracker.tracker;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;
import ru.spbstu.booktracker.user.ReadingListItem;

/** {@code /quotes [n]}: list quotes per book or for all books (Requirements §3.6). */
@Component
public class QuotesHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public QuotesHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "quotes";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String chatIdStr = String.valueOf(chatId);
    String arg = argument == null ? "" : argument.trim();
    if (arg.isEmpty()) {
      sendAll(chatId, chatIdStr);
      return;
    }
    var idxOpt = Validation.parseIndex(arg);
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер из списка.");
      return;
    }
    var item = tracker.findByIndex(chatIdStr, idxOpt.get());
    if (item.isEmpty()) {
      telegram.sendMessage(chatId, "Книга с таким ID не найдена в вашем списке.");
      return;
    }
    sendForBook(chatId, idxOpt.get(), item.get());
  }

  private void sendAll(long chatId, String chatIdStr) {
    var fullList = tracker.fullList(chatIdStr);
    var sb = new StringBuilder();
    boolean any = false;
    for (int i = 0; i < fullList.size(); i++) {
      var it = fullList.get(i);
      if (it.getQuotes().isEmpty()) {
        continue;
      }
      any = true;
      sb.append(i + 1).append(". <b>").append(TextUtils.escapeHtml(it.getTitle())).append("</b>");
      if (it.getAuthor() != null && !it.getAuthor().isBlank()) {
        sb.append(" — ").append(TextUtils.escapeHtml(it.getAuthor()));
      }
      sb.append('\n');
      for (String q : it.getQuotes()) {
        sb.append("  • ").append(TextUtils.escapeHtml(q)).append('\n');
      }
    }
    if (!any) {
      telegram.sendMessage(
          chatId,
          "Сохранённых цитат нет. Добавьте через <code>/quote add &lt;номер&gt; &lt;текст&gt;</code>.");
      return;
    }
    telegram.sendMessage(chatId, sb.toString());
  }

  private void sendForBook(long chatId, int oneBasedIdx, ReadingListItem item) {
    if (item.getQuotes().isEmpty()) {
      telegram.sendMessage(chatId, "Для этой книги нет сохранённых цитат.");
      return;
    }
    var sb = new StringBuilder();
    sb.append("<b>Цитаты — ").append(TextUtils.escapeHtml(item.getTitle())).append("</b>\n");
    for (String q : item.getQuotes()) {
      sb.append("• ").append(TextUtils.escapeHtml(q)).append('\n');
    }
    telegram.sendMessage(chatId, sb.toString());
  }
}
