package ru.spbstu.booktracker.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Telegram {@code CallbackQuery} object (subset). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackQuery {

  private String id;
  private String data;
  private Message message;
  private long fromId;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public long getFromId() {
    return fromId;
  }

  public void setFromId(long fromId) {
    this.fromId = fromId;
  }
}
