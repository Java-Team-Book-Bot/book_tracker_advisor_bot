package ru.spbstu.booktracker.preferences;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** Implements {@code /preferences add|show|remove}. */
@Component
public class PreferencesHandler implements CommandHandler {

  private final PreferencesService service;
  private final TelegramClient telegram;

  public PreferencesHandler(PreferencesService service, TelegramClient telegram) {
    this.service = service;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "preferences";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String chatIdStr = String.valueOf(chatId);

    String[] parts = argument.split("\\s+", 2);
    String sub = parts[0].toLowerCase(Locale.ROOT);
    String tail = parts.length > 1 ? parts[1] : "";

    switch (sub) {
      case "add" -> handleAdd(chatId, chatIdStr, tail);
      case "show" -> handleShow(chatId, chatIdStr);
      case "remove" -> handleRemove(chatId, chatIdStr, tail);
      default ->
          telegram.sendMessage(
              chatId,
              "Используйте: /preferences add &lt;текст&gt;, /preferences show, /preferences remove &lt;номер&gt;.");
    }
  }

  private void handleAdd(long chatId, String chatIdStr, String text) {
    var result = service.add(chatIdStr, text.trim());
    switch (result) {
      case OK ->
          telegram.sendMessage(
              chatId, "Предпочтение «" + TextUtils.escapeHtml(text.trim()) + "» добавлено!");
      case MISSING_TEXT ->
          telegram.sendMessage(
              chatId,
              "Укажите жанр или автора. Пример использования: <code>/preferences add антиутопия</code>.");
      case TOO_LONG ->
          telegram.sendMessage(
              chatId,
              "Ваш запрос длиннее 50 символов, пожалуйста, сократите его и повторите попытку.");
      case LIMIT_REACHED ->
          telegram.sendMessage(
              chatId,
              "Достигнут лимит предпочтений (15). Пожалуйста, удалите неактуальные записи с помощью"
                  + " <code>/preferences remove &lt;номер&gt;</code>.");
      default -> {
        // exhaustive
      }
    }
  }

  private void handleShow(long chatId, String chatIdStr) {
    List<String> prefs = service.list(chatIdStr);
    if (prefs.isEmpty()) {
      telegram.sendMessage(
          chatId,
          "У вас пока нет сохранённых предпочтений. Добавьте их с помощью команды"
              + " <code>/preferences add &lt;жанр/автор&gt;</code>.");
      return;
    }
    var sb = new StringBuilder("<b>Ваши предпочтения:</b>\n");
    for (int i = 0; i < prefs.size(); i++) {
      sb.append(i + 1).append(". ").append(TextUtils.escapeHtml(prefs.get(i))).append('\n');
    }
    telegram.sendMessage(chatId, sb.toString());
  }

  private void handleRemove(long chatId, String chatIdStr, String numberArg) {
    var idx = Validation.parseIndex(numberArg.trim());
    if (idx.isEmpty()) {
      telegram.sendMessage(
          chatId,
          "Укажите корректный номер из списка. Пример: <code>/preferences remove 1</code>.");
      return;
    }
    var result = service.remove(chatIdStr, idx.get());
    if (!result.removed()) {
      telegram.sendMessage(
          chatId,
          "Предпочтение с таким номером не найдено. Пожалуйста, сверьтесь с вашим списком:"
              + " <code>/preferences show</code>.");
      return;
    }
    telegram.sendMessage(
        chatId, "Предпочтение «" + TextUtils.escapeHtml(result.value()) + "» успешно удалено.");
  }
}
