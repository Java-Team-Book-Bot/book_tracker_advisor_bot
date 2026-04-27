package ru.spbstu.booktracker.config;

import jakarta.annotation.PreDestroy;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Embedded Jetty 12 server bootstrap. Wires the existing Spring root context as the parent of the
 * web application context so all controllers/beans are visible to {@link DispatcherServlet}.
 */
@Component
public class JettyServer {

  private static final Logger log = LoggerFactory.getLogger(JettyServer.class);

  private final ApplicationContext rootContext;
  private final EnvProperties env;
  private Server server;

  @Autowired
  public JettyServer(ApplicationContext rootContext, EnvProperties env) {
    this.rootContext = rootContext;
    this.env = env;
  }

  @EventListener(ContextRefreshedEvent.class)
  public synchronized void start() throws Exception {
    if (server != null) {
      return;
    }
    int port = env.getInt("HTTP_PORT", 8080);
    server = new Server(port);

    var contextHandler = new ServletContextHandler();
    contextHandler.setContextPath("/");

    var webContext = new AnnotationConfigWebApplicationContext();
    webContext.setParent(rootContext);
    webContext.register(WebConfig.class);
    webContext.scan("ru.spbstu.booktracker.http");

    var dispatcher = new DispatcherServlet(webContext);
    var holder = new ServletHolder("dispatcher", dispatcher);
    holder.setInitOrder(1);
    contextHandler.addServlet(holder, "/");

    server.setHandler(contextHandler);
    server.start();
    log.info("Jetty server started on port {}", port);
  }

  @PreDestroy
  public void stop() throws Exception {
    if (server != null && server.isRunning()) {
      log.info("Stopping Jetty server");
      server.stop();
    }
  }
}
