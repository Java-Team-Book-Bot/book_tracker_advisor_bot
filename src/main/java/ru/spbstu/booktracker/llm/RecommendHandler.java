package ru.spbstu.booktracker.llm;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;
import ru.spbstu.booktracker.user.BookStatus;
import ru.spbstu.booktracker.user.UserService;

/** {@code /recommend}: 3-book personalised picks (Requirements §3.5). */
@Component
public class RecommendHandler implements CommandHandler {

  private final UserService userService;
  private final PromptBuilder prompts;
  private final LlmAdvisorService llm;
  private final TelegramClient telegram;

  public RecommendHandler(
      UserService userService,
      PromptBuilder prompts,
      LlmAdvisorService llm,
      TelegramClient telegram) {
    this.userService = userService;
    this.prompts = prompts;
    this.llm = llm;
    this.telegram = telegram;
  }

  @Override
  public String command() {
    return "recommend";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    var user = userService.findOrCreate(String.valueOf(chatId));
    boolean noPrefs = user.getPreferences().isEmpty();
    boolean noHistory =
        user.getReadingList().stream()
            .noneMatch(
                it ->
                    it.getStatus() == BookStatus.FINISHED || it.getStatus() == BookStatus.READING);
    if (noPrefs && noHistory) {
      telegram.sendMessage(
          chatId,
          "Для точных рекомендаций мне нужно лучше узнать ваши вкусы. Оцените пару книг или"
              + " заполните предпочтения через команду <code>/preferences add</code>.");
      return;
    }
    llm.submit(chatId, prompts.recommend(user));
  }
}
