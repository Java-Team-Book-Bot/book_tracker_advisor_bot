package ru.spbstu.booktracker.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

class UserRepositoryIT {

  private static MongoDBContainer mongo;
  private static AnnotationConfigApplicationContext ctx;
  private UserRepository repository;

  @BeforeAll
  static void startMongo() {
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Skipping MongoDB integration test: Docker daemon is not available.");
    mongo = new MongoDBContainer(DockerImageName.parse("mongo:7"));
    mongo.start();
    System.setProperty("MONGO_URI", mongo.getReplicaSetUrl("booktracker_it"));
    ctx = new AnnotationConfigApplicationContext(TestMongoConfig.class);
  }

  @AfterAll
  static void stopMongo() {
    if (ctx != null) {
      ctx.close();
    }
    if (mongo != null) {
      mongo.stop();
    }
  }

  @BeforeEach
  void setUp() {
    repository = ctx.getBean(UserRepository.class);
    repository.deleteAll();
  }

  @Test
  void saveAndFindByChatId() {
    var u = new User("12345");
    u.getPreferences().add("Sci-Fi");
    repository.save(u);
    var found = repository.findByChatId("12345");
    assertThat(found).isPresent();
    assertThat(found.get().getPreferences()).containsExactly("Sci-Fi");
  }

  @Test
  void findUsersWithReminderAt() {
    var u1 = new User("1");
    u1.setReminder(new Reminder(true, "21:00"));
    var u2 = new User("2");
    u2.setReminder(new Reminder(true, "08:00"));
    var u3 = new User("3");
    u3.setReminder(new Reminder(false, "21:00"));
    repository.save(u1);
    repository.save(u2);
    repository.save(u3);
    var due = repository.findUsersWithReminderAt("21:00");
    assertThat(due).extracting(User::getChatId).containsExactly("1");
  }

  @Configuration
  @EnableMongoRepositories(basePackageClasses = UserRepository.class)
  static class TestMongoConfig {

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
      String uri = System.getProperty("MONGO_URI");
      var conn = new ConnectionString(uri);
      String db = conn.getDatabase() != null ? conn.getDatabase() : "booktracker_it";
      return new SimpleMongoClientDatabaseFactory(MongoClients.create(conn), db);
    }

    @Bean
    public org.springframework.data.mongodb.core.MongoTemplate mongoTemplate(
        MongoDatabaseFactory factory) {
      return new org.springframework.data.mongodb.core.MongoTemplate(factory);
    }
  }
}
