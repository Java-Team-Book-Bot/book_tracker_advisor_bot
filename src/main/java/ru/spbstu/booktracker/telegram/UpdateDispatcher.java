package ru.spbstu.booktracker.telegram;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.dto.Update;
import ru.spbstu.booktracker.user.UserService;

/** Top-level entry point for an incoming Telegram update. */
@Component
public class UpdateDispatcher {

  private final CommandRouter commandRouter;
  private final CallbackRouter callbackRouter;
  private final TelegramClient telegram;
  private final UserService userService;

  public UpdateDispatcher(
      CommandRouter commandRouter,
      CallbackRouter callbackRouter,
      TelegramClient telegram,
      UserService userService) {
    this.commandRouter = commandRouter;
    this.callbackRouter = callbackRouter;
    this.telegram = telegram;
    this.userService = userService;
  }

  public void dispatch(Update update) {
    if (update.getMessage() != null) {
      handleMessage(update.getMessage());
    } else if (update.getCallbackQuery() != null) {
      var cb = update.getCallbackQuery();
      if (cb.getMessage() != null && cb.getMessage().getChat() != null) {
        long chatId = cb.getMessage().getChat().getId();
        MDC.put("chatId", String.valueOf(chatId));
        try {
          userService.findOrCreate(String.valueOf(chatId));
          callbackRouter.dispatch(cb);
        } finally {
          MDC.remove("chatId");
        }
      }
    }
  }

  private void handleMessage(Message message) {
    if (message.getChat() == null) {
      return;
    }
    long chatId = message.getChat().getId();
    MDC.put("chatId", String.valueOf(chatId));
    try {
      userService.findOrCreate(String.valueOf(chatId));
      if (message.isNonText()) {
        telegram.sendMessage(
            chatId, "Пожалуйста, используйте текстовые команды. Введите /help для списка команд");
        return;
      }
      String text = message.getText();
      if (text == null || text.isBlank()) {
        return;
      }
      if (text.startsWith("/")) {
        commandRouter.dispatch(message);
      } else {
        telegram.sendMessage(chatId, "Не понимаю обычный текст. Введите /help для списка команд.");
      }
    } finally {
      MDC.remove("chatId");
    }
  }
}
