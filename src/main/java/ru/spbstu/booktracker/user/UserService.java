package ru.spbstu.booktracker.user;

import java.util.List;
import org.springframework.stereotype.Service;

/** Single point of access to user data — every other module must go through this service. */
@Service
public class UserService {

  private final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  /** Returns existing user or creates a new one for the given chat. */
  public User findOrCreate(String chatId) {
    return repository.findByChatId(chatId).orElseGet(() -> repository.save(new User(chatId)));
  }

  public User save(User user) {
    return repository.save(user);
  }

  public List<User> findAll() {
    return repository.findAll();
  }

  public List<User> findUsersWithReminderAt(String hhmm) {
    return repository.findUsersWithReminderAt(hhmm);
  }
}
