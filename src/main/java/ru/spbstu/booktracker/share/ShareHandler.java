package ru.spbstu.booktracker.share;

import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.config.EnvProperties;
import ru.spbstu.booktracker.telegram.TelegramClient;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.handlers.CommandHandler;

/** {@code /share}: returns a deep-link to the user's read-only list. */
@Component
public class ShareHandler implements CommandHandler {

  private final TelegramClient telegram;
  private final EnvProperties env;

  public ShareHandler(TelegramClient telegram, EnvProperties env) {
    this.telegram = telegram;
    this.env = env;
  }

  @Override
  public String command() {
    return "share";
  }

  @Override
  public void handle(Message message, String argument) {
    long chatId = message.getChat().getId();
    String botUsername = env.getOrDefault("BOT_USERNAME", "BookAdvisorBot");
    String url = "https://t.me/" + botUsername + "?start=list_" + chatId;
    telegram.sendMessage(
        chatId,
        "Поделитесь этой ссылкой с друзьями — они смогут просматривать ваш список:\n"
            + "<a href=\""
            + url
            + "\">"
            + url
            + "</a>");
  }
}
