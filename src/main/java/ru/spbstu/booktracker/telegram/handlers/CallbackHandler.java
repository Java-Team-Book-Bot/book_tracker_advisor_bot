package ru.spbstu.booktracker.telegram.handlers;

import ru.spbstu.booktracker.telegram.dto.CallbackQuery;

/** Handler for an inline-keyboard callback. */
public interface CallbackHandler {

  /**
   * Returns the callback-data prefix this handler accepts (e.g. {@code "search:"}). Convention:
   * tokens are separated by {@code ':'}.
   */
  String prefix();

  void handle(CallbackQuery callback);
}
