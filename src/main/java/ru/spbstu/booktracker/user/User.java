package ru.spbstu.booktracker.user;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Mongo document for the {@code users} collection (matches JSON-schema in architecture). */
@Document(collection = "users")
public class User {

  @Id private String id;

  @Indexed(unique = true)
  private String chatId;

  private List<ReadingListItem> readingList = new ArrayList<>();

  private List<String> preferences = new ArrayList<>();

  private Reminder reminder = new Reminder();

  public User() {}

  public User(String chatId) {
    this.chatId = chatId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getChatId() {
    return chatId;
  }

  public void setChatId(String chatId) {
    this.chatId = chatId;
  }

  public List<ReadingListItem> getReadingList() {
    if (readingList == null) {
      readingList = new ArrayList<>();
    }
    return readingList;
  }

  public void setReadingList(List<ReadingListItem> readingList) {
    this.readingList = readingList != null ? readingList : new ArrayList<>();
  }

  public List<String> getPreferences() {
    if (preferences == null) {
      preferences = new ArrayList<>();
    }
    return preferences;
  }

  public void setPreferences(List<String> preferences) {
    this.preferences = preferences != null ? preferences : new ArrayList<>();
  }

  public Reminder getReminder() {
    if (reminder == null) {
      reminder = new Reminder();
    }
    return reminder;
  }

  public void setReminder(Reminder reminder) {
    this.reminder = reminder != null ? reminder : new Reminder();
  }
}
