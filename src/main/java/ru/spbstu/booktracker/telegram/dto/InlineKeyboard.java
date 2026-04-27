package ru.spbstu.booktracker.telegram.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Helper builder for the {@code InlineKeyboardMarkup} payload. */
public final class InlineKeyboard {

  private final List<List<Button>> rows = new ArrayList<>();

  public static InlineKeyboard create() {
    return new InlineKeyboard();
  }

  public InlineKeyboard row(Button... buttons) {
    rows.add(Arrays.asList(buttons));
    return this;
  }

  public List<List<Button>> rows() {
    return rows;
  }

  public boolean isEmpty() {
    return rows.isEmpty();
  }

  /** Inline keyboard button. */
  public record Button(String text, String callback_data) {

    public static Button of(String text, String data) {
      return new Button(text, data);
    }
  }
}
