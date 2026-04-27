package ru.spbstu.booktracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.spbstu.booktracker.http.AdminAuthInterceptor;

/** Spring MVC configuration. Activated when running inside the embedded servlet container. */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

  private final AdminAuthInterceptor adminAuthInterceptor;
  private final ObjectMapper objectMapper;

  @Autowired
  public WebConfig(AdminAuthInterceptor adminAuthInterceptor, ObjectMapper objectMapper) {
    this.adminAuthInterceptor = adminAuthInterceptor;
    this.objectMapper = objectMapper;
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/users", "/users/**");
  }
}
