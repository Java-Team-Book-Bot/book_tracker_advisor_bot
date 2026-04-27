package ru.spbstu.booktracker.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextUtilsTest {

  @Test
  void escapeHtml_replacesSpecials() {
    assertThat(TextUtils.escapeHtml("<a> & </a>")).isEqualTo("&lt;a&gt; &amp; &lt;/a&gt;");
  }

  @Test
  void escapeHtml_handlesNull() {
    assertThat(TextUtils.escapeHtml(null)).isEmpty();
  }

  @Test
  void truncate_shortStaysSame() {
    assertThat(TextUtils.truncate("abc", 10)).isEqualTo("abc");
  }

  @Test
  void truncate_longGetsCut() {
    assertThat(TextUtils.truncate("abcdef", 4)).hasSize(4).endsWith("…");
  }
}
