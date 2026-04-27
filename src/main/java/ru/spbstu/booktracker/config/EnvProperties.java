package ru.spbstu.booktracker.config;

import java.util.Optional;
import org.springframework.stereotype.Component;

/** Wrapper around process environment / system properties for testability. */
@Component
public class EnvProperties {

  /** Returns the value or throws if missing/blank. */
  public String required(String key) {
    return optional(key)
        .orElseThrow(
            () -> new IllegalStateException("Missing required environment variable: " + key));
  }

  public String getOrDefault(String key, String fallback) {
    return optional(key).orElse(fallback);
  }

  public int getInt(String key, int fallback) {
    return optional(key).map(Integer::parseInt).orElse(fallback);
  }

  public Optional<String> optional(String key) {
    var sys = System.getProperty(key);
    if (sys != null && !sys.isBlank()) {
      return Optional.of(sys);
    }
    var env = System.getenv(key);
    if (env != null && !env.isBlank()) {
      return Optional.of(env);
    }
    return Optional.empty();
  }
}
