package by.chaika19.userservice;

import by.chaika19.userservice.repository.PaymentCardRepository;
import by.chaika19.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.liquibase.enabled=true"
        }
)
@AutoConfigureTestRestTemplate
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:15-alpine");

    @SuppressWarnings("resource")
    protected static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PaymentCardRepository paymentCardRepository;

    @Autowired
    protected CacheManager cacheManager;

    @BeforeEach
    void clearDatabaseAndCache() {
        try {
            paymentCardRepository.deleteAllInBatch();
            userRepository.deleteAllInBatch();

            if (cacheManager != null) {
                cacheManager.getCacheNames().forEach(cacheName ->
                        Objects.requireNonNull(cacheManager.getCache(cacheName)).clear()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Test cleanup failed. Aborting to prevent dirty database or cache state.", e);
        }
    }
}