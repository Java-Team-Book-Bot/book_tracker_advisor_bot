package ru.spbstu.booktracker.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Telegram {@code Update} object (subset). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Update {

  private long updateId;
  private Message message;
  private CallbackQuery callbackQuery;

  public long getUpdateId() {
    return updateId;
  }

  public void setUpdateId(long updateId) {
    this.updateId = updateId;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public CallbackQuery getCallbackQuery() {
    return callbackQuery;
  }

  public void setCallbackQuery(CallbackQuery callbackQuery) {
    this.callbackQuery = callbackQuery;
  }
}
