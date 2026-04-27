package ru.spbstu.booktracker.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.spbstu.booktracker.config.EnvProperties;

/**
 * Long-poll worker thread: continuously calls {@code getUpdates} and forwards updates to {@link
 * UpdateDispatcher}.
 */
@Component
public class TelegramPoller {

  private static final Logger log = LoggerFactory.getLogger(TelegramPoller.class);

  private final TelegramClient telegram;
  private final UpdateDispatcher dispatcher;
  private final EnvProperties env;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread thread;
  private long offset;

  public TelegramPoller(TelegramClient telegram, UpdateDispatcher dispatcher, EnvProperties env) {
    this.telegram = telegram;
    this.dispatcher = dispatcher;
    this.env = env;
  }

  @PostConstruct
  public void start() {
    if (env.optional("TELEGRAM_BOT_TOKEN").isEmpty()) {
      log.warn("Telegram poller is disabled because TELEGRAM_BOT_TOKEN is not set");
      return;
    }
    if (!running.compareAndSet(false, true)) {
      return;
    }
    thread = new Thread(this::loop, "telegram-poller");
    thread.setDaemon(true);
    thread.start();
    log.info("Telegram long-poll worker started");
  }

  @PreDestroy
  public void stop() {
    running.set(false);
    if (thread != null) {
      thread.interrupt();
    }
  }

  private void loop() {
    while (running.get()) {
      try {
        var updates = telegram.getUpdates(offset);
        for (var u : updates) {
          try {
            dispatcher.dispatch(u);
          } catch (RuntimeException ex) {
            log.error("Update {} failed", u.getUpdateId(), ex);
          } finally {
            offset = Math.max(offset, u.getUpdateId() + 1);
          }
        }
      } catch (RuntimeException ex) {
        log.warn("Polling error: {}", ex.getMessage());
        sleepQuiet(2000);
      }
    }
  }

  private void sleepQuiet(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
