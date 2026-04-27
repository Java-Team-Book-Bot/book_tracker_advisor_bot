package ru.spbstu.booktracker.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Mongo configuration based on {@link AbstractMongoClientConfiguration} so that the {@code
 * mongoTemplate} bean is wired in the canonical Spring Data way.
 */
@Configuration
@EnableMongoRepositories(basePackages = "ru.spbstu.booktracker")
public class MongoConfig extends AbstractMongoClientConfiguration {

  private final EnvProperties env;

  public MongoConfig(EnvProperties env) {
    this.env = env;
  }

  @Override
  protected String getDatabaseName() {
    String uri = env.getOrDefault("MONGO_URI", "mongodb://localhost:27017/booktracker");
    var connection = new ConnectionString(uri);
    String db = connection.getDatabase();
    return (db == null || db.isBlank()) ? "booktracker" : db;
  }

  @Override
  public MongoClient mongoClient() {
    String uri = env.getOrDefault("MONGO_URI", "mongodb://localhost:27017/booktracker");
    return MongoClients.create(new ConnectionString(uri));
  }

  @Override
  protected Collection<String> getMappingBasePackages() {
    return List.of("ru.spbstu.booktracker");
  }

  @Override
  protected boolean autoIndexCreation() {
    // Defer index creation to first use so the app can start even when MongoDB is briefly
    // unavailable (e.g. during compose start-up ordering).
    return false;
  }
}
