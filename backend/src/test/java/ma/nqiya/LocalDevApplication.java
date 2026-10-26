package ma.nqiya;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

/**
 * Zero-setup local run: starts a real embedded PostgreSQL (data kept in {@code target/dev-pg}) and the API on :8080.
 * Run with {@code mvn spring-boot:test-run}. For Docker or an existing database use {@link NqiyaApplication}.
 */
public final class LocalDevApplication {

    private LocalDevApplication() {
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of("target", "dev-pg");
        Files.createDirectories(dataDir);
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setDataDirectory(dataDir)
                .setCleanDataDirectory(false)
                .setPort(54329)
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pg.close();
            } catch (IOException ignored) {
                // shutting down anyway
            }
        }));

        System.setProperty("spring.datasource.url", pg.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        SpringApplication.run(NqiyaApplication.class, args);
    }
}
