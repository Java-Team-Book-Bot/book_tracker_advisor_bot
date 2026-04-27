package ru.spbstu.booktracker.search;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory store of active {@link SearchSession}s keyed by {@code chatId+messageId}. Sessions are
 * short-lived; lost on restart by design.
 */
@Component
public class SearchSessionStore {

  private final ConcurrentHashMap<String, SearchSession> sessions = new ConcurrentHashMap<>();

  public static String key(long chatId, long messageId) {
    return chatId + ":" + messageId;
  }

  public void put(long chatId, long messageId, SearchSession session) {
    sessions.put(key(chatId, messageId), session);
  }

  public SearchSession get(long chatId, long messageId) {
    return sessions.get(key(chatId, messageId));
  }

  public void remove(long chatId, long messageId) {
    sessions.remove(key(chatId, messageId));
  }
}
