package ru.spbstu.booktracker.tracker;

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
import ru.spbstu.booktracker.search.BookMetadata;
import ru.spbstu.booktracker.user.BookStatus;
import ru.spbstu.booktracker.user.ReadingListItem;
import ru.spbstu.booktracker.user.User;
import ru.spbstu.booktracker.user.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrackerServiceTest {

  private static final String CHAT_ID = "1";

  @Mock private UserService userService;

  @InjectMocks private TrackerService tracker;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User(CHAT_ID);
    when(userService.findOrCreate(CHAT_ID)).thenReturn(user);
    when(userService.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void addBook_added() {
    var result = tracker.addBook(CHAT_ID, book("b1"), BookStatus.TO_READ);
    assertThat(result.outcome()).isEqualTo(TrackerService.AddOutcome.ADDED);
    assertThat(user.getReadingList()).hasSize(1);
  }

  @Test
  void addBook_duplicate() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.TO_READ);
    var result = tracker.addBook(CHAT_ID, book("b1"), BookStatus.READING);
    assertThat(result.outcome()).isEqualTo(TrackerService.AddOutcome.ALREADY_PRESENT);
    assertThat(result.existingStatus()).isEqualTo(BookStatus.TO_READ);
  }

  @Test
  void addBook_limit() {
    for (int i = 0; i < TrackerService.LIST_LIMIT; i++) {
      var item = new ReadingListItem("b" + i, "T" + i, null, null, null);
      item.setStatus(BookStatus.TO_READ);
      user.getReadingList().add(item);
    }
    var result = tracker.addBook(CHAT_ID, book("new"), BookStatus.TO_READ);
    assertThat(result.outcome()).isEqualTo(TrackerService.AddOutcome.LIMIT_REACHED);
  }

  @Test
  void updateProgress_wrongStatus() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.TO_READ);
    var result = tracker.updateProgress(CHAT_ID, 1, "100");
    assertThat(result.outcome()).isEqualTo(TrackerService.ProgressOutcome.WRONG_STATUS);
  }

  @Test
  void updateProgress_ok() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.READING);
    var result = tracker.updateProgress(CHAT_ID, 1, "120");
    assertThat(result.outcome()).isEqualTo(TrackerService.ProgressOutcome.OK);
    assertThat(result.item().getProgress()).isEqualTo("120");
  }

  @Test
  void rate_onlyForFinished() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.READING);
    var bad = tracker.rate(CHAT_ID, 1, 9);
    assertThat(bad.outcome()).isEqualTo(TrackerService.FinishedOpOutcome.WRONG_STATUS);

    tracker.changeStatus(CHAT_ID, 1, BookStatus.FINISHED);
    var ok = tracker.rate(CHAT_ID, 1, 9);
    assertThat(ok.outcome()).isEqualTo(TrackerService.FinishedOpOutcome.OK);
    assertThat(ok.item().getRating()).isEqualTo(9);
  }

  @Test
  void rate_outOfRange() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.FINISHED);
    assertThat(tracker.rate(CHAT_ID, 1, 0).outcome())
        .isEqualTo(TrackerService.FinishedOpOutcome.INVALID);
    assertThat(tracker.rate(CHAT_ID, 1, 11).outcome())
        .isEqualTo(TrackerService.FinishedOpOutcome.INVALID);
  }

  @Test
  void quote_limit() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.FINISHED);
    for (int i = 0; i < TrackerService.QUOTES_PER_BOOK; i++) {
      tracker.addQuote(CHAT_ID, 1, "q" + i);
    }
    var extra = tracker.addQuote(CHAT_ID, 1, "q-extra");
    assertThat(extra.outcome()).isEqualTo(TrackerService.QuoteOutcome.LIMIT_REACHED);
  }

  @Test
  void quote_tooLong() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.FINISHED);
    String over = "x".repeat(501);
    assertThat(tracker.addQuote(CHAT_ID, 1, over).outcome())
        .isEqualTo(TrackerService.QuoteOutcome.TOO_LONG);
  }

  @Test
  void remove_ok() {
    tracker.addBook(CHAT_ID, book("b1"), BookStatus.TO_READ);
    var removed = tracker.remove(CHAT_ID, 1);
    assertThat(removed).isPresent();
    assertThat(user.getReadingList()).isEmpty();
  }

  private BookMetadata book(String id) {
    return new BookMetadata(id, "Title-" + id, "Author", "2024", "Description");
  }
}
