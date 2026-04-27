package ru.spbstu.booktracker.search;

import java.util.List;

/** State of a paginated search shown in a single Telegram message. */
public class SearchSession {

  private final String query;
  private int startIndex;
  private List<BookMetadata> page;

  /** True when this session represents a shared user's reading list (read-only). */
  private final boolean readOnly;

  /** Owner chat-id when {@link #readOnly} is true. */
  private final String ownerChatId;

  /** Pre-loaded full list for read-only sessions. {@code null} for live searches. */
  private List<BookMetadata> staticItems;

  public SearchSession(String query) {
    this(query, false, null);
  }

  public SearchSession(String query, boolean readOnly, String ownerChatId) {
    this.query = query;
    this.readOnly = readOnly;
    this.ownerChatId = ownerChatId;
  }

  public List<BookMetadata> getStaticItems() {
    return staticItems;
  }

  public void setStaticItems(List<BookMetadata> staticItems) {
    this.staticItems = staticItems;
  }

  public String getQuery() {
    return query;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public List<BookMetadata> getPage() {
    return page;
  }

  public void setPage(List<BookMetadata> page) {
    this.page = page;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public String getOwnerChatId() {
    return ownerChatId;
  }
}
