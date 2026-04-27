package ru.spbstu.booktracker.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ru.spbstu.booktracker.telegram.InProgressRegistry.Kind;

class InProgressRegistryTest {

  @Test
  void tryAcquire_blocksDuplicates() {
    var reg = new InProgressRegistry();
    assertThat(reg.tryAcquire(1, Kind.LLM)).isTrue();
    assertThat(reg.tryAcquire(1, Kind.LLM)).isFalse();
    reg.release(1, Kind.LLM);
    assertThat(reg.tryAcquire(1, Kind.LLM)).isTrue();
  }

  @Test
  void tryAcquire_independentKinds() {
    var reg = new InProgressRegistry();
    assertThat(reg.tryAcquire(1, Kind.LLM)).isTrue();
    assertThat(reg.tryAcquire(1, Kind.SEARCH)).isTrue();
  }

  @Test
  void tryAcquire_independentChats() {
    var reg = new InProgressRegistry();
    assertThat(reg.tryAcquire(1, Kind.LLM)).isTrue();
    assertThat(reg.tryAcquire(2, Kind.LLM)).isTrue();
  }
}
