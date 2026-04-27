package ru.spbstu.booktracker.llm;

import java.util.List;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.user.BookStatus;
import ru.spbstu.booktracker.user.ReadingListItem;
import ru.spbstu.booktracker.user.User;

/** Builds system + user prompts for the four LLM use-cases. */
@Component
public class PromptBuilder {

  private static final String LITERARY_CRITIC_ROLE =
      "Ты — внимательный литературный критик и помощник, отвечающий по-русски. "
          + "Отвечай только на вопросы о книгах, авторах, жанрах и литературе. "
          + "Если вопрос не связан с книгами, вежливо откажись отвечать. "
          + "Будь лаконичен, структурируй ответ.";

  /** /recommend. */
  public Prompt recommend(User user) {
    var sb = new StringBuilder();
    sb.append("Сделай мне подборку из 3 книг с обоснованием. Учти мой контекст.\n\n");
    sb.append(formatPreferences(user.getPreferences()));
    sb.append(formatReadingHistory(user.getReadingList()));
    return new Prompt(LITERARY_CRITIC_ROLE, sb.toString());
  }

  /** /similar. */
  public Prompt similar(ReadingListItem item) {
    String user =
        "Подбери 3 книги, похожие по стилю, атмосфере и жанру на «"
            + safe(item.getTitle())
            + "»"
            + (item.getAuthor() == null ? "" : " (" + safe(item.getAuthor()) + ")")
            + ". Для каждой объясни в 1-2 предложениях, в чём сходство.";
    return new Prompt(LITERARY_CRITIC_ROLE, user);
  }

  /** Summary callback. */
  public Prompt summary(String title, String author) {
    String user =
        "Составь краткое саммари книги «"
            + safe(title)
            + "»"
            + (author == null ? "" : " автора " + safe(author))
            + ": кратко перескажи сюжет (без главных спойлеров) и обозначь ключевые идеи. "
            + "Уложись в 5-7 предложений.";
    return new Prompt(LITERARY_CRITIC_ROLE, user);
  }

  /** /ask. */
  public Prompt ask(String question) {
    return new Prompt(LITERARY_CRITIC_ROLE, safe(question));
  }

  private String formatPreferences(List<String> prefs) {
    if (prefs == null || prefs.isEmpty()) {
      return "Предпочтения: не указаны.\n";
    }
    var sb = new StringBuilder("Предпочтения пользователя: ");
    for (int i = 0; i < prefs.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(safe(prefs.get(i)));
    }
    sb.append('\n');
    return sb.toString();
  }

  private String formatReadingHistory(List<ReadingListItem> list) {
    if (list == null || list.isEmpty()) {
      return "История чтения пуста.\n";
    }
    var sb = new StringBuilder("История чтения:\n");
    for (var item : list) {
      if (item.getStatus() != BookStatus.FINISHED && item.getStatus() != BookStatus.READING) {
        continue;
      }
      sb.append("- ").append(safe(item.getTitle()));
      if (item.getAuthor() != null) {
        sb.append(" (").append(safe(item.getAuthor())).append(')');
      }
      if (item.getStatus() != null) {
        sb.append(" [").append(item.getStatus().code()).append(']');
      }
      if (item.getRating() != null) {
        sb.append(" оценка ").append(item.getRating()).append("/10");
      }
      if (item.getNote() != null && !item.getNote().isBlank()) {
        sb.append(" заметка: ").append(safe(item.getNote()));
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  /** Removes characters that may break JSON encoding upstream. */
  private String safe(String s) {
    if (s == null) {
      return "";
    }
    return s.replace('\u0000', ' ').replace("\r", "").trim();
  }

  /** System + user prompt pair. */
  public record Prompt(String system, String user) {}
}
