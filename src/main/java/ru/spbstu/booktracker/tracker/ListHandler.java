package ru.spbstu.booktracker.tracker;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;
import ru.spbstu.booktracker.user.BookStatus;
import ru.spbstu.booktracker.user.ReadingListItem;

/** Implements {@code /list [status]} (Requirements §3.3). */
@Component
public class ListHandler implements CommandHandler {

  private final TrackerService tracker;
  private final TelegramClient telegram;

  public ListHandler(TrackerService tracker, TelegramClient telegram) {
    this.tracker = tracker;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "list";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String chatIdStr = String.valueOf(chatId);
    BookStatus filter = null;
    if (argument != null && !argument.isBlank()) {
      var opt = BookStatus.fromCode(argument.trim().toLowerCase(Locale.ROOT));
      if (opt.isEmpty()) {
        telegram.sendMessage(
            chatId, "Укажите корректный статус. Доступные варианты: to_read, reading, finished.");
        return;
      }
      filter = opt.get();
    }
    var fullList = tracker.fullList(chatIdStr);
    if (fullList.isEmpty()) {
      telegram.sendMessage(
          chatId,
          "Ваш список чтения пока пуст. Найдите интересные книги с помощью команды"
              + " <code>/search &lt;запрос&gt;</code>.");
      return;
    }
    List<Integer> indexes = new java.util.ArrayList<>();
    for (int i = 0; i < fullList.size(); i++) {
      if (filter == null || filter.equals(fullList.get(i).getStatus())) {
        indexes.add(i);
      }
    }
    if (indexes.isEmpty()) {
      telegram.sendMessage(chatId, "У вас нет книг со статусом «" + filter.code() + "».");
      return;
    }
    var sb = new StringBuilder();
    sb.append("<b>Ваш список:</b>\n");
    for (int i : indexes) {
      sb.append(formatItem(i + 1, fullList.get(i)));
    }
    telegram.sendMessage(chatId, sb.toString());
  }

  private String formatItem(int oneBasedIdx, ReadingListItem item) {
    var sb = new StringBuilder();
    sb.append(oneBasedIdx).append(". ");
    if (item.getAuthor() != null && !item.getAuthor().isBlank()) {
      sb.append(TextUtils.escapeHtml(item.getAuthor())).append(" — ");
    }
    sb.append("<b>").append(TextUtils.escapeHtml(item.getTitle())).append("</b>");
    if (item.getStatus() != null) {
      sb.append(" — <i>").append(item.getStatus().label()).append("</i>");
    }
    sb.append('\n');
    if (item.getStatus() == BookStatus.READING && item.getProgress() != null) {
      sb.append("    Прогресс: ").append(TextUtils.escapeHtml(item.getProgress())).append('\n');
    }
    if (item.getStatus() == BookStatus.FINISHED) {
      if (item.getRating() != null) {
        sb.append("    Оценка: ").append(item.getRating()).append("/10\n");
      }
      if (item.getNote() != null && !item.getNote().isBlank()) {
        sb.append("    Заметка: ").append(TextUtils.escapeHtml(item.getNote())).append('\n');
      }
    }
    return sb.toString();
  }
}
