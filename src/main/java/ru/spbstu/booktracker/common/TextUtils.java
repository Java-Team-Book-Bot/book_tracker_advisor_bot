package ru.spbstu.booktracker.common;

/** Small text utilities (HTML escaping for Telegram parse_mode=HTML, etc). */
public final class TextUtils {

  private TextUtils() {}

  /** Escapes characters that are special in Telegram HTML parse mode. */
  public static String escapeHtml(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  /** Truncates string to {@code max} characters, appending an ellipsis when cut. */
  public static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, Math.max(0, max - 1)) + "…";
  }
}
