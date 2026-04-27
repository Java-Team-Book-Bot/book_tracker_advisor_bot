package ru.spbstu.booktracker.user;

/** Daily reminder configuration. */
public class Reminder {

  private boolean enabled;

  /** HH:mm format. */
  private String time;

  public Reminder() {}

  public Reminder(boolean enabled, String time) {
    this.enabled = enabled;
    this.time = time;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }
}
