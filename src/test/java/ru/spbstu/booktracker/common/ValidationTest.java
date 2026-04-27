package ru.spbstu.booktracker.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidationTest {

  @Test
  void parseIndex_validRange() {
    assertThat(Validation.parseIndex("1")).contains(1);
    assertThat(Validation.parseIndex("200")).contains(200);
    assertThat(Validation.parseIndex("999")).contains(999);
  }

  @Test
  void parseIndex_rejectsZero() {
    assertThat(Validation.parseIndex("0")).isEmpty();
  }

  @Test
  void parseIndex_rejectsNegative() {
    assertThat(Validation.parseIndex("-1")).isEmpty();
  }

  @Test
  void parseIndex_rejectsTooLarge() {
    assertThat(Validation.parseIndex("1000")).isEmpty();
  }

  @Test
  void parseIndex_rejectsLetters() {
    assertThat(Validation.parseIndex("a1")).isEmpty();
  }

  @Test
  void progress_validValues() {
    assertThat(Validation.isValidProgress("120")).isTrue();
    assertThat(Validation.isValidProgress("Глава 5")).isTrue();
    assertThat(Validation.isValidProgress("Chapter 12")).isTrue();
  }

  @Test
  void progress_rejectsSpecialCharacters() {
    assertThat(Validation.isValidProgress("page <1>")).isFalse();
    assertThat(Validation.isValidProgress("p@ge")).isFalse();
  }

  @Test
  void progress_rejectsTooLong() {
    String s = "a".repeat(51);
    assertThat(Validation.isValidProgress(s)).isFalse();
  }

  @Test
  void hhmm_valid() {
    assertThat(Validation.isValidHhmm("00:00")).isTrue();
    assertThat(Validation.isValidHhmm("23:59")).isTrue();
    assertThat(Validation.isValidHhmm("09:30")).isTrue();
  }

  @Test
  void hhmm_invalid() {
    assertThat(Validation.isValidHhmm("24:00")).isFalse();
    assertThat(Validation.isValidHhmm("9:30")).isFalse();
    assertThat(Validation.isValidHhmm("12-30")).isFalse();
  }
}
