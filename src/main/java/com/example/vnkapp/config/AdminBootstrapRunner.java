package com.example.vnkapp.config;

import com.example.vnkapp.entity.Admin;
import com.example.vnkapp.enums.admin.AdminRole;
import com.example.vnkapp.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${app.admin.bootstrap.email:}")
    private String bootstrapEmail;

    @Value("${app.admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Value("${app.admin.bootstrap.full-name:Super Admin}")
    private String bootstrapFullName;

    public AdminBootstrapRunner(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            return;
        }

        if (adminRepository.count() > 0) {
            log.debug("Admin bootstrap skipped - admins already exist");
            return;
        }

        if (!StringUtils.hasText(bootstrapUsername)
                || !StringUtils.hasText(bootstrapEmail)
                || !StringUtils.hasText(bootstrapPassword)) {
            log.warn("Admin bootstrap enabled but username, email, or password is missing");
            return;
        }

        Admin superAdmin = Admin.builder()
                .username(bootstrapUsername.trim())
                .email(bootstrapEmail.trim().toLowerCase())
                .password(passwordEncoder.encode(bootstrapPassword))
                .fullName(bootstrapFullName)
                .role(AdminRole.SUPER_ADMIN)
                .build();

        adminRepository.save(superAdmin);
        log.info("Bootstrap super admin created with username: {}", superAdmin.getUsername());
    }
}
