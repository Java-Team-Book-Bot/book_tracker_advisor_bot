package ru.spbstu.booktracker.tracker;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.spbstu.booktracker.search.BookMetadata;
import ru.spbstu.booktracker.user.BookStatus;
import ru.spbstu.booktracker.user.ReadingListItem;
import ru.spbstu.booktracker.user.User;
import ru.spbstu.booktracker.user.UserService;

/** Business logic for the user's reading list (Requirements §3.3, §3.6). */
@Service
public class TrackerService {

  public static final int LIST_LIMIT = 200;
  public static final int NOTE_MAX = 300;
  public static final int QUOTE_MAX = 500;
  public static final int QUOTES_PER_BOOK = 10;
  public static final int PROGRESS_MAX = 50;

  private final UserService userService;

  public TrackerService(UserService userService) {
    this.userService = userService;
  }

  /** Outcome of {@link #addBook}. */
  public enum AddOutcome {
    ADDED,
    ALREADY_PRESENT,
    LIMIT_REACHED
  }

  /** Result of an add. */
  public record AddResult(AddOutcome outcome, BookStatus existingStatus) {}

  /** Adds a book or returns the existing status. */
  public AddResult addBook(String chatId, BookMetadata book, BookStatus status) {
    User user = userService.findOrCreate(chatId);
    Optional<ReadingListItem> existing =
        user.getReadingList().stream()
            .filter(it -> it.getBookId().equals(book.bookId()))
            .findFirst();
    if (existing.isPresent()) {
      return new AddResult(AddOutcome.ALREADY_PRESENT, existing.get().getStatus());
    }
    if (user.getReadingList().size() >= LIST_LIMIT) {
      return new AddResult(AddOutcome.LIMIT_REACHED, null);
    }
    var item =
        new ReadingListItem(
            book.bookId(), book.title(), book.author(), book.year(), book.description());
    item.setStatus(status);
    user.getReadingList().add(item);
    userService.save(user);
    return new AddResult(AddOutcome.ADDED, status);
  }

  public List<ReadingListItem> list(String chatId, BookStatus filter) {
    User user = userService.findOrCreate(chatId);
    if (filter == null) {
      return List.copyOf(user.getReadingList());
    }
    return user.getReadingList().stream().filter(it -> filter.equals(it.getStatus())).toList();
  }

  /** Returns the underlying full list (allows index-based commands). */
  public List<ReadingListItem> fullList(String chatId) {
    return List.copyOf(userService.findOrCreate(chatId).getReadingList());
  }

  /** Resolves an item by 1-based index in the *full* list. */
  public Optional<ReadingListItem> findByIndex(String chatId, int oneBasedIndex) {
    var items = userService.findOrCreate(chatId).getReadingList();
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= items.size()) {
      return Optional.empty();
    }
    return Optional.of(items.get(idx));
  }

  public boolean changeStatus(String chatId, int oneBasedIndex, BookStatus newStatus) {
    User user = userService.findOrCreate(chatId);
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= user.getReadingList().size()) {
      return false;
    }
    var item = user.getReadingList().get(idx);
    item.setStatus(newStatus);
    if (newStatus != BookStatus.READING) {
      item.setProgress(null);
    }
    if (newStatus != BookStatus.FINISHED) {
      item.setRating(null);
      item.setNote(null);
    }
    userService.save(user);
    return true;
  }

  /** Outcome of {@link #updateProgress}. */
  public enum ProgressOutcome {
    OK,
    NOT_FOUND,
    WRONG_STATUS
  }

  public record ProgressResult(ProgressOutcome outcome, ReadingListItem item) {}

  public ProgressResult updateProgress(String chatId, int oneBasedIndex, String value) {
    User user = userService.findOrCreate(chatId);
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= user.getReadingList().size()) {
      return new ProgressResult(ProgressOutcome.NOT_FOUND, null);
    }
    var item = user.getReadingList().get(idx);
    if (item.getStatus() != BookStatus.READING) {
      return new ProgressResult(ProgressOutcome.WRONG_STATUS, item);
    }
    item.setProgress(value);
    userService.save(user);
    return new ProgressResult(ProgressOutcome.OK, item);
  }

  public Optional<ReadingListItem> remove(String chatId, int oneBasedIndex) {
    User user = userService.findOrCreate(chatId);
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= user.getReadingList().size()) {
      return Optional.empty();
    }
    var removed = user.getReadingList().remove(idx);
    userService.save(user);
    return Optional.of(removed);
  }

  /** Outcome of rating/note operations. */
  public enum FinishedOpOutcome {
    OK,
    NOT_FOUND,
    WRONG_STATUS,
    INVALID
  }

  public record RateResult(FinishedOpOutcome outcome, ReadingListItem item) {}

  public RateResult rate(String chatId, int oneBasedIndex, int rating) {
    if (rating < 1 || rating > 10) {
      return new RateResult(FinishedOpOutcome.INVALID, null);
    }
    User user = userService.findOrCreate(chatId);
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= user.getReadingList().size()) {
      return new RateResult(FinishedOpOutcome.NOT_FOUND, null);
    }
    var item = user.getReadingList().get(idx);
    if (item.getStatus() != BookStatus.FINISHED) {
      return new RateResult(FinishedOpOutcome.WRONG_STATUS, item);
    }
    item.setRating(rating);
    userService.save(user);
    return new RateResult(FinishedOpOutcome.OK, item);
  }

  public RateResult note(String chatId, int oneBasedIndex, String text) {
    if (text == null || text.length() > NOTE_MAX) {
      return new RateResult(FinishedOpOutcome.INVALID, null);
    }
    User user = userService.findOrCreate(chatId);
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= user.getReadingList().size()) {
      return new RateResult(FinishedOpOutcome.NOT_FOUND, null);
    }
    var item = user.getReadingList().get(idx);
    if (item.getStatus() != BookStatus.FINISHED) {
      return new RateResult(FinishedOpOutcome.WRONG_STATUS, item);
    }
    item.setNote(text);
    userService.save(user);
    return new RateResult(FinishedOpOutcome.OK, item);
  }

  /** Outcome of {@link #addQuote}. */
  public enum QuoteOutcome {
    OK,
    NOT_FOUND,
    LIMIT_REACHED,
    TOO_LONG
  }

  public record QuoteResult(QuoteOutcome outcome, ReadingListItem item) {}

  public QuoteResult addQuote(String chatId, int oneBasedIndex, String quote) {
    if (quote == null || quote.isBlank()) {
      return new QuoteResult(QuoteOutcome.TOO_LONG, null);
    }
    if (quote.length() > QUOTE_MAX) {
      return new QuoteResult(QuoteOutcome.TOO_LONG, null);
    }
    User user = userService.findOrCreate(chatId);
    int idx = oneBasedIndex - 1;
    if (idx < 0 || idx >= user.getReadingList().size()) {
      return new QuoteResult(QuoteOutcome.NOT_FOUND, null);
    }
    var item = user.getReadingList().get(idx);
    if (item.getQuotes().size() >= QUOTES_PER_BOOK) {
      return new QuoteResult(QuoteOutcome.LIMIT_REACHED, item);
    }
    item.getQuotes().add(quote);
    userService.save(user);
    return new QuoteResult(QuoteOutcome.OK, item);
  }
}
