package ru.spbstu.booktracker.telegram.handlers;

import ru.spbstu.booktracker.telegram.dto.Message;

/** Handler for a Telegram slash command (e.g. {@code /search}). */
public interface CommandHandler {

  /** Lower-case command without the leading slash. */
  String command();

  /** Process the command. {@code argument} is the text after the command (may be empty). */
  void handle(Message message, String argument);
}
