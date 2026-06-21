package com.example.vnkapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs lightweight ALTER TABLE statements that ddl-auto:update cannot handle
 * (dropping NOT NULL constraints from existing columns).
 * All statements are idempotent — safe to run on every startup.
 */
@Component
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running schema migrations...");
        alterUserMedicationsNullable();
        log.info("Schema migrations completed.");
    }

    private void alterUserMedicationsNullable() {
        // dosage, frequency, start_date are temporarily nullable until the full
        // medication detail flow (dosage, schedule) is implemented.
        jdbcTemplate.execute("ALTER TABLE user_medications ALTER COLUMN dosage DROP NOT NULL");
        jdbcTemplate.execute("ALTER TABLE user_medications ALTER COLUMN frequency DROP NOT NULL");
        jdbcTemplate.execute("ALTER TABLE user_medications ALTER COLUMN start_date DROP NOT NULL");
        log.debug("user_medications: dosage, frequency, start_date made nullable");
    }
}
