package ru.spbstu.booktracker.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthCheckControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new HealthCheckController()).build();
  }

  @Test
  void returnsStatusUpAndAuthors() throws Exception {
    var result =
        mockMvc
            .perform(get("/healthcheck"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andReturn();
    String body = result.getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(body).contains("\"status\":\"UP\"");
    org.assertj.core.api.Assertions.assertThat(body).contains("\"authors\":[");
  }
}
