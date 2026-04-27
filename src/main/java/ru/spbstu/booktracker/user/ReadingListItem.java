package ru.spbstu.booktracker.user;

import java.util.ArrayList;
import java.util.List;

/** Single book entry inside a user's reading list. */
public class ReadingListItem {

  private String bookId;
  private String title;
  private String author;
  private String year;
  private String description;
  private BookStatus status;

  /** Page or chapter (only for status=reading). */
  private String progress;

  /** 1..10 (only for status=finished). */
  private Integer rating;

  /** Personal note (only for status=finished, max 300 chars). */
  private String note;

  /** Up to 10 favourite quotes (each max 500 chars). */
  private List<String> quotes = new ArrayList<>();

  public ReadingListItem() {}

  public ReadingListItem(
      String bookId, String title, String author, String year, String description) {
    this.bookId = bookId;
    this.title = title;
    this.author = author;
    this.year = year;
    this.description = description;
  }

  public String getBookId() {
    return bookId;
  }

  public void setBookId(String bookId) {
    this.bookId = bookId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BookStatus getStatus() {
    return status;
  }

  public void setStatus(BookStatus status) {
    this.status = status;
  }

  public String getProgress() {
    return progress;
  }

  public void setProgress(String progress) {
    this.progress = progress;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<String> getQuotes() {
    if (quotes == null) {
      quotes = new ArrayList<>();
    }
    return quotes;
  }

  public void setQuotes(List<String> quotes) {
    this.quotes = quotes != null ? quotes : new ArrayList<>();
  }
}
