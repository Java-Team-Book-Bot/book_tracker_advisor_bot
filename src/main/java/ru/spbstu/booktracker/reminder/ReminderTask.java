package ru.spbstu.booktracker.reminder;

/** Wire payload pushed onto the ZeroMQ queue. */
public record ReminderTask(long chatId, String text) {}
