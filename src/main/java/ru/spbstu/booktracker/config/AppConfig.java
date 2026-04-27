package ru.spbstu.booktracker.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root (non-web) application configuration. Excludes the HTTP controllers and {@link WebConfig} so
 * that they are loaded later by the embedded Jetty's {@code DispatcherServlet} into a dedicated
 * {@link org.springframework.web.context.WebApplicationContext}.
 */
@Configuration
@EnableScheduling
@ComponentScan(
    basePackages = "ru.spbstu.booktracker",
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = {RestController.class, Controller.class}),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = {WebConfig.class})
    })
public class AppConfig {}
