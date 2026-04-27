package ru.spbstu.booktracker.telegram.handlers;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;

/** /help and /start: short usage cheatsheet. */
@Component
public class HelpHandler implements CommandHandler {

  private static final String HELP_TEXT =
      """
      <b>Book Tracker and Advisor Bot</b>

      <b>Поиск книг</b>
      /search &lt;запрос&gt; — поиск по запросу (до 100 символов)
      /search — поиск по сохранённым предпочтениям
      /random — случайная книга

      <b>Список чтения</b>
      /list [статус] — показать список (статусы: хочу прочитать, читаю, прочитано)
      /status &lt;номер&gt; &lt;новый_статус&gt; — изменить статус
      /progress &lt;номер&gt; &lt;значение&gt; — обновить прогресс (только для "читаю")
      /remove &lt;номер&gt; — удалить книгу
      /rate &lt;номер&gt; &lt;1-10&gt; — оценить (только прочитанные)
      /note &lt;номер&gt; &lt;текст&gt; — заметка (только прочитанные)
      /quote add &lt;номер&gt; &lt;текст&gt; — добавить цитату
      /quotes [номер] — показать цитаты

      <b>Предпочтения</b>
      /preferences add &lt;текст&gt;
      /preferences show
      /preferences remove &lt;номер&gt;

      <b>LLM-советник</b>
      /recommend — персональные рекомендации
      /similar &lt;номер&gt; — похожие книги
      /ask &lt;вопрос&gt; — свободный вопрос (до 500 символов)

      <b>Прочее</b>
      /share — поделиться списком
      /remind on &lt;ЧЧ:ММ&gt; | /remind off — напоминания
      """;

  private final TelegramClient telegram;

  public HelpHandler(TelegramClient telegram) {
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "help";
  }

  @Override
  public void handle(Message message, String argument) {
    telegram.sendMessage(message.getChat().getId(), HELP_TEXT);
  }
}
