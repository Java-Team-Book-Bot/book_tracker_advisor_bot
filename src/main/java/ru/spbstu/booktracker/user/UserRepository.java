package ru.spbstu.booktracker.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/** Mongo repository for {@link User} documents. */
public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByChatId(String chatId);

  @Query("{ 'reminder.enabled': true, 'reminder.time': ?0 }")
  List<User> findUsersWithReminderAt(String time);
}
