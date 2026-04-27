package ru.spbstu.booktracker.user;

import java.util.Optional;

/** Reading status of a book in the user's reading list. */
public enum BookStatus {
  TO_READ("to_read", "Хочу прочитать"),
  READING("reading", "Читаю"),
  FINISHED("finished", "Прочитано");

  private final String code;
  private final String label;

  BookStatus(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public static Optional<BookStatus> fromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    for (BookStatus s : values()) {
      if (s.code.equalsIgnoreCase(code)) {
        return Optional.of(s);
      }
    }
    return fromRussian(code);
  }

  private static Optional<BookStatus> fromRussian(String code) {
    return switch (code.toLowerCase(java.util.Locale.ROOT)) {
      case "хочу прочитать" -> Optional.of(TO_READ);
      case "читаю" -> Optional.of(READING);
      case "прочитано" -> Optional.of(FINISHED);
      default -> Optional.empty();
    };
  }
}
