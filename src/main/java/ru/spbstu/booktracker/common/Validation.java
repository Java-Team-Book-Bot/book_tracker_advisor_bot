package ru.spbstu.booktracker.common;

import java.util.Optional;
import java.util.regex.Pattern;

/** Centralized validation patterns and helpers (all from Requirements §4.4). */
public final class Validation {

  /** Positive integer up to 3 digits — for list indexes (limit 200). */
  public static final Pattern INDEX_PATTERN = Pattern.compile("^[1-9]\\d{0,2}$");

  /** Letters / digits / spaces, 1..50 chars — for /progress value. */
  public static final Pattern PROGRESS_PATTERN = Pattern.compile("^[a-zA-Zа-яА-ЯёЁ0-9\\s]{1,50}$");

  /** Reminder time HH:mm. */
  public static final Pattern HHMM_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

  private Validation() {}

  public static Optional<Integer> parseIndex(String s) {
    if (s == null || !INDEX_PATTERN.matcher(s).matches()) {
      return Optional.empty();
    }
    return Optional.of(Integer.parseInt(s));
  }

  public static boolean isValidProgress(String s) {
    return s != null && PROGRESS_PATTERN.matcher(s).matches();
  }

  public static boolean isValidHhmm(String s) {
    return s != null && HHMM_PATTERN.matcher(s).matches();
  }
}
