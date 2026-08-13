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
        updatePaymentsPaymentMethodConstraint();
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

    private void updatePaymentsPaymentMethodConstraint() {
        // Hibernate ddl-auto:update does not refresh enum CHECK constraints when new
        // PaymentMethod values (e.g. NGENIUS) are added to the Java enum.
        jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check");
        jdbcTemplate.execute("""
                ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
                CHECK (payment_method IN (
                    'CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'NET_BANKING',
                    'WALLET', 'COD', 'RAZORPAY', 'STRIPE', 'NGENIUS'
                ))
                """);
        log.debug("payments: payment_method check constraint updated to include NGENIUS");
    }
}
