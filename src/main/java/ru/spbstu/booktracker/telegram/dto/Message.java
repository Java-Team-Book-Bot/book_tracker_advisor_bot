package ru.spbstu.booktracker.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Telegram {@code Message} object (subset). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

  private long messageId;
  private Chat chat;
  private String text;

  /** Present when the message is non-text (photo, sticker, document, voice, etc.). */
  private boolean nonText;

  public long getMessageId() {
    return messageId;
  }

  public void setMessageId(long messageId) {
    this.messageId = messageId;
  }

  public Chat getChat() {
    return chat;
  }

  public void setChat(Chat chat) {
    this.chat = chat;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public boolean isNonText() {
    return nonText;
  }

  public void setNonText(boolean nonText) {
    this.nonText = nonText;
  }
}
