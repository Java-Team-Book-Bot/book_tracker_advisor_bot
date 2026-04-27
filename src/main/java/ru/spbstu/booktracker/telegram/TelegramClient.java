package ru.spbstu.booktracker.telegram;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.spbstu.booktracker.config.EnvProperties;
import ru.spbstu.booktracker.telegram.dto.Chat;
import ru.spbstu.booktracker.telegram.dto.InlineKeyboard;
import ru.spbstu.booktracker.telegram.dto.Message;
import ru.spbstu.booktracker.telegram.dto.Update;

/** Thin wrapper over Telegram Bot HTTP API based on Spring {@link RestClient}. */
@Component
public class TelegramClient {

  private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);
  private static final int LONG_POLL_TIMEOUT_SECONDS = 30;

  private final RestClient client;

  public TelegramClient(EnvProperties env) {
    String token = env.optional("TELEGRAM_BOT_TOKEN").orElse("");
    if (token.isBlank()) {
      log.warn("TELEGRAM_BOT_TOKEN is not set; Telegram client will be unable to make calls");
    }
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(10));
    factory.setReadTimeout(Duration.ofSeconds(LONG_POLL_TIMEOUT_SECONDS + 5));
    this.client =
        RestClient.builder()
            .baseUrl("https://api.telegram.org/bot" + token)
            .requestFactory(factory)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  /** Long-poll {@code getUpdates}. Returns the next batch starting from {@code offset}. */
  public List<Update> getUpdates(long offset) {
    try {
      var body =
          Map.of(
              "offset",
              offset,
              "timeout",
              LONG_POLL_TIMEOUT_SECONDS,
              "allowed_updates",
              List.of("message", "callback_query"));
      JsonNode root = client.post().uri("/getUpdates").body(body).retrieve().body(JsonNode.class);
      if (root == null || !root.path("ok").asBoolean()) {
        log.warn("getUpdates returned not-ok: {}", root);
        return List.of();
      }
      List<Update> updates = new ArrayList<>();
      for (JsonNode node : root.withArray("result")) {
        updates.add(convertUpdate(node));
      }
      return updates;
    } catch (RuntimeException ex) {
      log.warn("getUpdates failed: {}", ex.getMessage());
      return List.of();
    }
  }

  private Update convertUpdate(JsonNode node) {
    var u = new Update();
    u.setUpdateId(node.path("update_id").asLong());
    if (node.has("message")) {
      u.setMessage(convertMessage(node.path("message")));
    }
    if (node.has("callback_query")) {
      var cb = new ru.spbstu.booktracker.telegram.dto.CallbackQuery();
      var c = node.path("callback_query");
      cb.setId(c.path("id").asText());
      cb.setData(c.path("data").asText(null));
      cb.setFromId(c.path("from").path("id").asLong());
      if (c.has("message")) {
        cb.setMessage(convertMessage(c.path("message")));
      }
      u.setCallbackQuery(cb);
    }
    return u;
  }

  private Message convertMessage(JsonNode node) {
    var m = new Message();
    m.setMessageId(node.path("message_id").asLong());
    var ch = new Chat();
    ch.setId(node.path("chat").path("id").asLong());
    ch.setType(node.path("chat").path("type").asText(null));
    ch.setUsername(node.path("chat").path("username").asText(null));
    ch.setFirstName(node.path("chat").path("first_name").asText(null));
    ch.setLastName(node.path("chat").path("last_name").asText(null));
    m.setChat(ch);
    if (node.has("text")) {
      m.setText(node.path("text").asText());
    }
    boolean nonText =
        !node.has("text")
            && (node.has("photo")
                || node.has("sticker")
                || node.has("document")
                || node.has("voice")
                || node.has("audio")
                || node.has("video")
                || node.has("video_note")
                || node.has("animation")
                || node.has("contact")
                || node.has("location"));
    m.setNonText(nonText);
    return m;
  }

  /** Sends a message and returns its message_id (or {@code -1} on failure). */
  public long sendMessage(long chatId, String text, InlineKeyboard keyboard) {
    var payload =
        new SendMessageRequest(chatId, text, "HTML", keyboard == null ? null : toMarkup(keyboard));
    try {
      JsonNode root =
          client.post().uri("/sendMessage").body(payload).retrieve().body(JsonNode.class);
      if (root == null || !root.path("ok").asBoolean()) {
        log.warn("sendMessage failed: {}", root);
        return -1;
      }
      return root.path("result").path("message_id").asLong();
    } catch (RuntimeException ex) {
      log.warn("sendMessage error: {}", ex.getMessage());
      return -1;
    }
  }

  public long sendMessage(long chatId, String text) {
    return sendMessage(chatId, text, null);
  }

  /** Edits an existing message text. */
  public boolean editMessageText(
      long chatId, long messageId, String text, InlineKeyboard keyboard) {
    var payload =
        new EditMessageRequest(
            chatId, messageId, text, "HTML", keyboard == null ? null : toMarkup(keyboard));
    try {
      JsonNode root =
          client.post().uri("/editMessageText").body(payload).retrieve().body(JsonNode.class);
      return root != null && root.path("ok").asBoolean();
    } catch (RuntimeException ex) {
      log.warn("editMessageText error: {}", ex.getMessage());
      return false;
    }
  }

  public void answerCallbackQuery(String callbackId) {
    answerCallbackQuery(callbackId, null);
  }

  public void answerCallbackQuery(String callbackId, String text) {
    var payload = new HashMap<String, Object>();
    payload.put("callback_query_id", callbackId);
    if (text != null) {
      payload.put("text", text);
    }
    try {
      client.post().uri("/answerCallbackQuery").body(payload).retrieve().toBodilessEntity();
    } catch (RuntimeException ex) {
      log.debug("answerCallbackQuery failed (ignored): {}", ex.getMessage());
    }
  }

  /** Deletes a message. */
  public void deleteMessage(long chatId, long messageId) {
    var payload = Map.of("chat_id", chatId, "message_id", messageId);
    try {
      client.post().uri("/deleteMessage").body(payload).retrieve().toBodilessEntity();
    } catch (RuntimeException ex) {
      log.debug("deleteMessage failed (ignored): {}", ex.getMessage());
    }
  }

  private Map<String, Object> toMarkup(InlineKeyboard keyboard) {
    return Map.of("inline_keyboard", keyboard.rows());
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record SendMessageRequest(
      @JsonProperty("chat_id") long chatId,
      String text,
      @JsonProperty("parse_mode") String parseMode,
      @JsonProperty("reply_markup") Map<String, Object> replyMarkup) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record EditMessageRequest(
      @JsonProperty("chat_id") long chatId,
      @JsonProperty("message_id") long messageId,
      String text,
      @JsonProperty("parse_mode") String parseMode,
      @JsonProperty("reply_markup") Map<String, Object> replyMarkup) {}
}
