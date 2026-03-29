package database.dao;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.concurrent.TimeUnit;


public class MongoConnectionPool {

    private static volatile MongoConnectionPool instance;
    private final MongoClient mongoClient;
    private final String databaseName;

    private MongoConnectionPool() {
        Dotenv dotenv = Dotenv.load();
        String connectionString = dotenv.get("MONGO_CONNECTION_STRING");
        this.databaseName = dotenv.get("MONGO_DATABASE_NAME", "FIISmart");

        if (connectionString == null || connectionString.isEmpty()) {
            throw new RuntimeException("MONGO_CONNECTION_STRING nu a fost găsită în .env");
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToConnectionPoolSettings(builder ->
                        builder
                                .maxSize(20)               // maxim 20 conexiuni în pool
                                .minSize(5)                // minim 5 conexiuni menținute active
                                .maxWaitTime(3000, TimeUnit.MILLISECONDS)  // timp maxim de așteptare pentru o conexiune
                                .maxConnectionLifeTime(30, TimeUnit.MINUTES)
                                .maxConnectionIdleTime(10, TimeUnit.MINUTES)
                )
                .applyToSocketSettings(builder ->
                        builder
                                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                                .readTimeout(10000, TimeUnit.MILLISECONDS)
                )
                .applyToServerSettings(builder ->
                        builder.heartbeatFrequency(10, TimeUnit.SECONDS)
                )
                .build();

        this.mongoClient = MongoClients.create(settings);
        System.out.println("✓ MongoDB connection pool inițializat (maxSize=20, minSize=5)");
    }

    public static MongoConnectionPool getInstance() {
        if (instance == null) {
            synchronized (MongoConnectionPool.class) {
                if (instance == null) {
                    instance = new MongoConnectionPool();
                }
            }
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return mongoClient.getDatabase(databaseName);
    }

    public MongoDatabase getDatabase(String dbName) {
        return mongoClient.getDatabase(dbName);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection pool închis.");
        }
    }
}
