package ru.spbstu.booktracker.http;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.ManualRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthCheckControllerTest {

  private final ManualRestDocumentation restDocumentation = new ManualRestDocumentation();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    restDocumentation.beforeTest(getClass(), "returnsStatusUpAndAuthors");
    mockMvc =
        MockMvcBuilders.standaloneSetup(new HealthCheckController())
            .apply(documentationConfiguration(restDocumentation))
            .build();
  }

  @AfterEach
  void tearDown() {
    restDocumentation.afterTest();
  }

  @Test
  void returnsStatusUpAndAuthors() throws Exception {
    mockMvc
        .perform(get("/healthcheck"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andDo(
            document(
                "healthcheck",
                responseFields(
                    fieldWithPath("status").description("The status of the application"),
                    fieldWithPath("authors").description("The list of project authors"))));
  }
}
