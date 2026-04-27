package ru.spbstu.booktracker.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.spbstu.booktracker.config.EnvProperties;

/**
 * Talks to GigaChat API: 30 s timeout, no retries (Requirements §4.2). OAuth token is cached and
 * refreshed when expired.
 */
@Component
public class GigaChatClient {

  /** Result kinds matching the prompts in {@link PromptBuilder}. */
  public enum Outcome {
    SUCCESS,
    TIMEOUT,
    API_ERROR
  }

  /** Result of a chat call. */
  public record ChatResponse(Outcome outcome, String text) {}

  private static final Logger log = LoggerFactory.getLogger(GigaChatClient.class);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  private final RestClient oauthClient;
  private final RestClient apiClient;
  private final String authKey;
  private final String scope;
  private final AtomicReference<TokenCache> token = new AtomicReference<>();

  public GigaChatClient(EnvProperties env) {
    this.authKey = env.optional("GIGACHAT_AUTH_KEY").orElse("");
    this.scope = env.getOrDefault("GIGACHAT_SCOPE", "GIGACHAT_API_PERS");
    var oauthFactory = new SimpleClientHttpRequestFactory();
    oauthFactory.setConnectTimeout(Duration.ofSeconds(10));
    oauthFactory.setReadTimeout(Duration.ofSeconds(15));
    this.oauthClient =
        RestClient.builder()
            .baseUrl("https://ngw.devices.sberbank.ru:9443/api/v2")
            .requestFactory(oauthFactory)
            .build();
    var apiFactory = new SimpleClientHttpRequestFactory();
    apiFactory.setConnectTimeout(Duration.ofSeconds(10));
    apiFactory.setReadTimeout(READ_TIMEOUT);
    this.apiClient =
        RestClient.builder()
            .baseUrl("https://gigachat.devices.sberbank.ru/api/v1")
            .requestFactory(apiFactory)
            .build();
  }

  /** Sends a chat completion request. Never throws — wraps everything into {@link ChatResponse}. */
  public ChatResponse chat(String systemPrompt, String userPrompt) {
    if (authKey.isBlank()) {
      log.warn("GIGACHAT_AUTH_KEY is not set; cannot call GigaChat");
      return new ChatResponse(Outcome.API_ERROR, null);
    }
    String accessToken;
    try {
      accessToken = ensureToken();
    } catch (RuntimeException ex) {
      log.warn("GigaChat OAuth failed: {}", ex.getMessage());
      return new ChatResponse(Outcome.API_ERROR, null);
    }
    var body =
        Map.of(
            "model",
            "GigaChat",
            "temperature",
            0.6,
            "messages",
            List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
    try {
      JsonNode root =
          apiClient
              .post()
              .uri("/chat/completions")
              .header("Authorization", "Bearer " + accessToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      if (root == null) {
        return new ChatResponse(Outcome.API_ERROR, null);
      }
      String content = root.path("choices").path(0).path("message").path("content").asText(null);
      if (content == null || content.isBlank()) {
        return new ChatResponse(Outcome.API_ERROR, null);
      }
      return new ChatResponse(Outcome.SUCCESS, content);
    } catch (RestClientException ex) {
      if (isTimeout(ex)) {
        return new ChatResponse(Outcome.TIMEOUT, null);
      }
      log.warn("GigaChat call failed: {}", ex.getMessage());
      return new ChatResponse(Outcome.API_ERROR, null);
    }
  }

  private boolean isTimeout(Throwable ex) {
    Throwable t = ex;
    while (t != null) {
      if (t instanceof java.net.SocketTimeoutException || t instanceof HttpTimeoutException) {
        return true;
      }
      t = t.getCause();
    }
    return false;
  }

  private String ensureToken() {
    var current = token.get();
    if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
      return current.value();
    }
    JsonNode root =
        oauthClient
            .post()
            .uri("/oauth")
            .header("Authorization", "Basic " + authKey)
            .header("RqUID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("scope=" + scope)
            .retrieve()
            .body(JsonNode.class);
    if (root == null || !root.has("access_token")) {
      throw new IllegalStateException("Empty OAuth response from GigaChat");
    }
    String value = root.path("access_token").asText();
    long expiresAtMs = root.path("expires_at").asLong(0);
    Instant expiresAt =
        expiresAtMs > 0 ? Instant.ofEpochMilli(expiresAtMs) : Instant.now().plusSeconds(1700);
    token.set(new TokenCache(value, expiresAt));
    return value;
  }

  private record TokenCache(String value, Instant expiresAt) {}
}
