package ru.spbstu.booktracker.telegram;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Tracks long-running operations per chat to block concurrent duplicates as required by section 4.3
 * of the requirements.
 */
@Component
public class InProgressRegistry {

  /** Categories of long-running operations. */
  public enum Kind {
    SEARCH,
    LLM
  }

  private final ConcurrentHashMap<Long, Set<Kind>> active = new ConcurrentHashMap<>();

  /** Atomically marks the kind active for a chat. Returns {@code false} if already running. */
  public boolean tryAcquire(long chatId, Kind kind) {
    var set = active.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet());
    return set.add(kind);
  }

  public void release(long chatId, Kind kind) {
    var set = active.get(chatId);
    if (set != null) {
      set.remove(kind);
      if (set.isEmpty()) {
        active.remove(chatId, set);
      }
    }
  }
}
