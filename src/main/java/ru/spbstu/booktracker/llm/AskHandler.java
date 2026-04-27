package ru.spbstu.booktracker.llm;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /ask <text>} — free-form question (Requirements §3.5). */
@Component
public class AskHandler implements CommandHandler {

  private static final int MAX_LENGTH = 500;

  private final PromptBuilder prompts;
  private final LlmAdvisorService llm;
  private final TelegramClient telegram;

  public AskHandler(PromptBuilder prompts, LlmAdvisorService llm, TelegramClient telegram) {
    this.prompts = prompts;
    this.llm = llm;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "ask";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String q = argument == null ? "" : argument.trim();
    if (q.isEmpty()) {
      telegram.sendMessage(
          chatId,
          "Сформулируйте вопрос. Пример: <code>/ask Что почитать в стиле киберпанка?</code>");
      return;
    }
    if (q.length() > MAX_LENGTH) {
      telegram.sendMessage(
          chatId,
          "Ваш вопрос слишком длинный (максимум 500 символов). Пожалуйста, сформулируйте его короче.");
      return;
    }
    llm.submit(chatId, prompts.ask(q));
  }
}
