package ru.spbstu.booktracker.llm;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.spbstu.booktracker.common.TextUtils;
import ru.spbstu.booktracker.llm.PromptBuilder.Prompt;
import ru.spbstu.booktracker.telegram.InProgressRegistry;
import ru.spbstu.booktracker.telegram.InProgressRegistry.Kind;
import ru.spbstu.booktracker.telegram.TelegramClient;

/**
 * Orchestrates the user-facing LLM flow with intermediate "Generating..." messages, timeout/error
 * edits, and concurrent-request blocking (Requirements §3.5.1).
 */
@Service
public class LlmAdvisorService {

  private static final Logger log = LoggerFactory.getLogger(LlmAdvisorService.class);
  private static final String GENERATING = "⏳ Генерирую ответ, пожалуйста, подождите...";
  private static final String TIMEOUT_MSG =
      "Превышено время ожидания ответа от нейросети. Пожалуйста, попробуйте позже.";
  private static final String API_ERROR_MSG =
      "Произошла ошибка на стороне сервиса GigaChat. Пожалуйста, попробуйте позже.";
  private static final String DUPLICATE_MSG = "Ваш предыдущий запрос ещё обрабатывается";

  private final GigaChatClient gigaChat;
  private final TelegramClient telegram;
  private final InProgressRegistry inProgress;
  private final ExecutorService executor;
  private final AtomicLong threadIdx = new AtomicLong();

  public LlmAdvisorService(
      GigaChatClient gigaChat, TelegramClient telegram, InProgressRegistry inProgress) {
    this.gigaChat = gigaChat;
    this.telegram = telegram;
    this.inProgress = inProgress;
    ThreadFactory tf =
        r -> {
          var t = new Thread(r, "llm-worker-" + threadIdx.incrementAndGet());
          t.setDaemon(true);
          return t;
        };
    this.executor = Executors.newCachedThreadPool(tf);
  }

  /**
   * Submits an LLM request asynchronously: sends the placeholder, calls GigaChat, edits the
   * placeholder with the result.
   *
   * @return {@code true} if the request was accepted, {@code false} if it was rejected as a
   *     duplicate.
   */
  public boolean submit(long chatId, Prompt prompt) {
    if (!inProgress.tryAcquire(chatId, Kind.LLM)) {
      long warn = telegram.sendMessage(chatId, DUPLICATE_MSG);
      scheduleAutoDelete(chatId, warn);
      return false;
    }
    long placeholder = telegram.sendMessage(chatId, GENERATING);
    if (placeholder <= 0) {
      inProgress.release(chatId, Kind.LLM);
      return false;
    }
    executor.submit(
        () -> {
          try {
            var response = gigaChat.chat(prompt.system(), prompt.user());
            String body =
                switch (response.outcome()) {
                  case SUCCESS -> TextUtils.escapeHtml(response.text());
                  case TIMEOUT -> TIMEOUT_MSG;
                  case API_ERROR -> API_ERROR_MSG;
                };
            telegram.editMessageText(chatId, placeholder, body, null);
          } catch (RuntimeException ex) {
            log.error("LLM call failed", ex);
            telegram.editMessageText(chatId, placeholder, API_ERROR_MSG, null);
          } finally {
            inProgress.release(chatId, Kind.LLM);
          }
        });
    return true;
  }

  private void scheduleAutoDelete(long chatId, long messageId) {
    if (messageId <= 0) {
      return;
    }
    var t =
        new Thread(
            () -> {
              try {
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
              }
              telegram.deleteMessage(chatId, messageId);
            },
            "llm-auto-delete-" + messageId);
    t.setDaemon(true);
    t.start();
  }

  @PreDestroy
  void shutdown() {
    executor.shutdownNow();
  }
}
