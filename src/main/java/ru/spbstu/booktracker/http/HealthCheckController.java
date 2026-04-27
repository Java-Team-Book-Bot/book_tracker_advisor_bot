package ru.spbstu.booktracker.http;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public {@code GET /healthcheck} endpoint, no auth required. */
@RestController
public class HealthCheckController {

  /** Project authors as required by the assignment. */
  private static final List<String> AUTHORS = List.of("Митяев О.", "Попов И.", "Фомин М.");

  @GetMapping("/healthcheck")
  public Map<String, Object> healthcheck() {
    return Map.of("status", "UP", "authors", AUTHORS);
  }
}
