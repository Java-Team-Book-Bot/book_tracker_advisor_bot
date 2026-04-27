package ru.spbstu.booktracker.search;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.spbstu.booktracker.config.EnvProperties;

/**
 * Client for the Google Books REST API: 5-second timeout with two exponentially-delayed retries
 * (Requirements §4.2).
 */
@Component
public class GoogleBooksClient {

  static final int PAGE_SIZE = 5;
  private static final Logger log = LoggerFactory.getLogger(GoogleBooksClient.class);
  private static final int MAX_ATTEMPTS = 3;
  private static final long INITIAL_BACKOFF_MS = 200L;

  private final RestClient client;
  private final String apiKey;

  public GoogleBooksClient(EnvProperties env) {
    this.apiKey = env.optional("GOOGLE_BOOKS_API_KEY").orElse("");
    // JDK HttpClient is more reliable than HttpURLConnection on slow / lossy networks.
    var httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_2)
            .build();
    var factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(5));
    this.client =
        RestClient.builder()
            .baseUrl("https://www.googleapis.com/books/v1")
            .requestFactory(factory)
            .build();
  }

  /**
   * Searches volumes. Returns up to {@link #PAGE_SIZE} entries starting from {@code startIndex}.
   * Empty list if everything fails.
   */
  public List<BookMetadata> search(String query, int startIndex) {
    return executeWithRetry(
        () -> {
          var spec =
              client
                  .get()
                  .uri(
                      uriBuilder ->
                          uriBuilder
                              .path("/volumes")
                              .queryParam("q", query)
                              .queryParam("maxResults", PAGE_SIZE)
                              .queryParam("startIndex", Math.max(0, startIndex))
                              .queryParamIfPresent(
                                  "key",
                                  apiKey.isBlank()
                                      ? java.util.Optional.empty()
                                      : java.util.Optional.of(apiKey))
                              .build());
          JsonNode root = spec.retrieve().body(JsonNode.class);
          return parseVolumes(root);
        });
  }

  /** Fetches a single volume by id. */
  public BookMetadata get(String bookId) {
    return executeWithRetry(
        () -> {
          JsonNode root =
              client
                  .get()
                  .uri(
                      uriBuilder ->
                          uriBuilder
                              .path("/volumes/{id}")
                              .queryParamIfPresent(
                                  "key",
                                  apiKey.isBlank()
                                      ? java.util.Optional.empty()
                                      : java.util.Optional.of(apiKey))
                              .build(bookId))
                  .retrieve()
                  .body(JsonNode.class);
          if (root == null) {
            return null;
          }
          return toBook(root);
        });
  }

  private List<BookMetadata> parseVolumes(JsonNode root) {
    if (root == null || !root.has("items")) {
      return List.of();
    }
    var out = new ArrayList<BookMetadata>();
    for (JsonNode item : root.withArray("items")) {
      out.add(toBook(item));
    }
    return out;
  }

  private BookMetadata toBook(JsonNode item) {
    String id = item.path("id").asText(null);
    JsonNode info = item.path("volumeInfo");
    String title = info.path("title").asText(null);
    String description = info.path("description").asText(null);
    String published = info.path("publishedDate").asText(null);
    String year =
        published != null && published.length() >= 4 ? published.substring(0, 4) : published;
    String author = null;
    if (info.has("authors") && info.path("authors").isArray() && info.path("authors").size() > 0) {
      var sb = new StringBuilder();
      for (JsonNode a : info.path("authors")) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(a.asText());
      }
      author = sb.toString();
    }
    return new BookMetadata(id, title, author, year, description);
  }

  private <T> T executeWithRetry(java.util.function.Supplier<T> call) {
    long delay = INITIAL_BACKOFF_MS;
    RuntimeException last = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return call.get();
      } catch (RuntimeException ex) {
        last = ex;
        log.warn(
            "Google Books call attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, ex.getMessage());
        if (attempt == MAX_ATTEMPTS) {
          break;
        }
        sleepQuiet(delay);
        delay *= 2;
      }
    }
    if (last != null) {
      throw new GoogleBooksException("Google Books request failed after retries", last);
    }
    return null;
  }

  private void sleepQuiet(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Wraps any failure to talk to Google Books. */
  public static class GoogleBooksException extends RuntimeException {
    public GoogleBooksException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
