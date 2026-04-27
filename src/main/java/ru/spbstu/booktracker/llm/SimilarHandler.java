package ru.spbstu.booktracker.llm;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.common.Validation;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;
import ru.spbstu.booktracker.tracker.TrackerService;

/** {@code /similar <номер>} (Requirements §3.5). */
@Component
public class SimilarHandler implements CommandHandler {

  private final TrackerService tracker;
  private final PromptBuilder prompts;
  private final LlmAdvisorService llm;
  private final TelegramClient telegram;

  public SimilarHandler(
      TrackerService tracker,
      PromptBuilder prompts,
      LlmAdvisorService llm,
      TelegramClient telegram) {
    this.tracker = tracker;
    this.prompts = prompts;
    this.llm = llm;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "similar";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    var idxOpt = Validation.parseIndex(argument == null ? "" : argument.trim());
    if (idxOpt.isEmpty()) {
      telegram.sendMessage(chatId, "Укажите корректный номер книги. Пример: /similar 3.");
      return;
    }
    var item = tracker.findByIndex(String.valueOf(chatId), idxOpt.get());
    if (item.isEmpty()) {
      telegram.sendMessage(chatId, "Книга с таким ID не найдена в вашем списке.");
      return;
    }
    llm.submit(chatId, prompts.similar(item.get()));
  }
}
