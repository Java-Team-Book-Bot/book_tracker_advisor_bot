package ru.spbstu.booktracker.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.spbstu.booktracker.config.EnvProperties;

/** Validates the {@code X-API-Key} header against the configured admin key. */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

  static final String API_KEY_HEADER = "X-API-Key";

  private final EnvProperties env;

  public AdminAuthInterceptor(EnvProperties env) {
    this.env = env;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String expected = env.getOrDefault("ADMIN_API_KEY", "");
    String actual = request.getHeader(API_KEY_HEADER);
    if (expected.isBlank() || actual == null || !expected.equals(actual)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"error\":\"Unauthorized\"}");
      return false;
    }
    return true;
  }
}
