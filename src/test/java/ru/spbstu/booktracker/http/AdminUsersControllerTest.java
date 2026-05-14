package ru.spbstu.booktracker.http;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.restdocs.ManualRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.spbstu.booktracker.config.EnvProperties;
import ru.spbstu.booktracker.user.User;
import ru.spbstu.booktracker.user.UserService;

@ExtendWith(MockitoExtension.class)
class AdminUsersControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  private final ManualRestDocumentation restDocumentation = new ManualRestDocumentation();
  private MockMvc mockMvc;

  @Mock private UserService userService;
  @Mock private EnvProperties env;

  @BeforeEach
  void setUp() {
    restDocumentation.beforeTest(getClass(), "listUsers_documentsResponse");
    when(env.getOrDefault("ADMIN_API_KEY", "")).thenReturn(ADMIN_KEY);
    AdminAuthInterceptor interceptor = new AdminAuthInterceptor(env);

    mockMvc =
        MockMvcBuilders.standaloneSetup(new AdminUsersController(userService))
            .addInterceptors(interceptor)
            .apply(documentationConfiguration(restDocumentation))
            .build();
  }

  @AfterEach
  void tearDown() {
    restDocumentation.afterTest();
  }

  @Test
  void listUsers_documentsResponse() throws Exception {
    User u = new User("12345");
    u.setId("user-id-1");
    when(userService.findAll()).thenReturn(List.of(u));

    mockMvc
        .perform(get("/users").header("X-API-Key", ADMIN_KEY))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users",
                requestHeaders(
                    headerWithName("X-API-Key").description("Admin API key for authorization")),
                responseFields(
                    fieldWithPath("[]").description("An array of users"),
                    fieldWithPath("[].id").description("The unique ID of the user"),
                    fieldWithPath("[].chatId").description("The Telegram chat ID of the user"),
                    fieldWithPath("[].readingListSize")
                        .description("Number of books in the reading list"),
                    fieldWithPath("[].preferencesCount").description("Number of saved preferences"),
                    fieldWithPath("[].reminderEnabled")
                        .description("Whether daily reminders are enabled"))));
  }
}
