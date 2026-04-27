package ru.spbstu.booktracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.spbstu.booktracker.config.AppConfig;

/** Application entry point: bootstraps Spring context and registers a JVM shutdown hook. */
public final class Application {

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private Application() {}

  public static void main(String[] args) {
    log.info("Starting Book Tracker and Advisor Bot...");
    var ctx = new AnnotationConfigApplicationContext(AppConfig.class);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutdown hook: closing Spring context");
                  ctx.close();
                },
                "app-shutdown-hook"));
    log.info("Application started successfully");
  }
}
