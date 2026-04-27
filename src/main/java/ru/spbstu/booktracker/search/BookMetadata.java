package ru.spbstu.booktracker.search;

/** Minimal book metadata used in search results and reading-list cards. */
public record BookMetadata(
    String bookId, String title, String author, String year, String description) {

  public String shortLine() {
    var sb = new StringBuilder();
    if (author != null && !author.isBlank()) {
      sb.append(author).append(" — ");
    }
    sb.append(title == null ? "(без названия)" : title);
    if (year != null && !year.isBlank()) {
      sb.append(" (").append(year).append(')');
    }
    return sb.toString();
  }
}
