package ru.spbstu.booktracker.reminder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import ru.spbstu.booktracker.config.EnvProperties;
import ru.spbstu.booktracker.telegram.TelegramClient;

/**
 * ZeroMQ adapter: PUSH (producer) and PULL (consumer worker) over the configured endpoint.
 * Implements the architecture's {@code ZeroMQReminderModule} (PDF §2.14, §2.20).
 */
@Component
public class ZeroMqReminderQueue {

  private static final Logger log = LoggerFactory.getLogger(ZeroMqReminderQueue.class);

  private final EnvProperties env;
  private final ObjectMapper mapper;
  private final TelegramClient telegram;
  private final AtomicBoolean running = new AtomicBoolean();
  private ZContext context;
  private ZMQ.Socket producer;
  private Thread workerThread;

  public ZeroMqReminderQueue(EnvProperties env, ObjectMapper mapper, TelegramClient telegram) {
    this.env = env;
    this.mapper = mapper;
    this.telegram = telegram;
  }

  @PostConstruct
  public void start() {
    String endpoint = env.getOrDefault("ZMQ_REMINDER_ENDPOINT", "tcp://127.0.0.1:5555");
    context = new ZContext();
    var bindEndpoint = endpoint;
    var connectEndpoint = endpoint;
    producer = context.createSocket(SocketType.PUSH);
    producer.bind(bindEndpoint);
    var consumer = context.createSocket(SocketType.PULL);
    consumer.connect(connectEndpoint);
    running.set(true);
    workerThread = new Thread(() -> consumeLoop(consumer), "reminder-zmq-worker");
    workerThread.setDaemon(true);
    workerThread.start();
    log.info("ZeroMQ reminder queue ready (endpoint={})", endpoint);
  }

  @PreDestroy
  public void stop() {
    running.set(false);
    if (workerThread != null) {
      workerThread.interrupt();
    }
    if (context != null) {
      context.close();
    }
  }

  /** Producer: enqueue a reminder. */
  public synchronized void publish(ReminderTask task) {
    if (producer == null) {
      log.warn("Producer not initialised; dropping reminder for chat {}", task.chatId());
      return;
    }
    try {
      producer.send(mapper.writeValueAsBytes(task));
    } catch (JsonProcessingException ex) {
      log.warn("Failed to serialise reminder", ex);
    }
  }

  private void consumeLoop(ZMQ.Socket consumer) {
    while (running.get()) {
      try {
        byte[] msg = consumer.recv(0);
        if (msg == null) {
          continue;
        }
        var task = mapper.readValue(msg, ReminderTask.class);
        telegram.sendMessage(task.chatId(), task.text());
      } catch (org.zeromq.ZMQException ex) {
        if (!running.get()) {
          return;
        }
        log.warn("ZMQ recv failed: {}", ex.getMessage());
      } catch (java.io.IOException ex) {
        log.warn("Failed to deserialise reminder", ex);
      }
    }
  }
}
