package ru.spbstu.booktracker.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.spbstu.booktracker.user.User;
import ru.spbstu.booktracker.user.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreferencesServiceTest {

  private static final String CHAT_ID = "42";

  @Mock private UserService userService;

  @InjectMocks private PreferencesService service;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User(CHAT_ID);
    when(userService.findOrCreate(CHAT_ID)).thenReturn(user);
    when(userService.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void add_ok() {
    var result = service.add(CHAT_ID, "Киберпанк");
    assertThat(result).isEqualTo(PreferencesService.AddResult.OK);
    assertThat(user.getPreferences()).containsExactly("Киберпанк");
  }

  @Test
  void add_missingText() {
    assertThat(service.add(CHAT_ID, "")).isEqualTo(PreferencesService.AddResult.MISSING_TEXT);
    assertThat(service.add(CHAT_ID, null)).isEqualTo(PreferencesService.AddResult.MISSING_TEXT);
  }

  @Test
  void add_tooLong() {
    String long51 = "a".repeat(51);
    assertThat(service.add(CHAT_ID, long51)).isEqualTo(PreferencesService.AddResult.TOO_LONG);
  }

  @Test
  void add_limit() {
    for (int i = 0; i < 15; i++) {
      user.getPreferences().add("p" + i);
    }
    assertThat(service.add(CHAT_ID, "extra")).isEqualTo(PreferencesService.AddResult.LIMIT_REACHED);
  }

  @Test
  void remove_ok() {
    user.getPreferences().add("a");
    user.getPreferences().add("b");
    var result = service.remove(CHAT_ID, 2);
    assertThat(result.removed()).isTrue();
    assertThat(result.value()).isEqualTo("b");
    assertThat(user.getPreferences()).containsExactly("a");
  }

  @Test
  void remove_notFound() {
    user.getPreferences().add("a");
    assertThat(service.remove(CHAT_ID, 5).removed()).isFalse();
  }
}
