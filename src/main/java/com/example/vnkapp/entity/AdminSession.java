package com.example.vnkapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_sessions", indexes = {
        @Index(name = "idx_admin_sessions_token", columnList = "session_token"),
        @Index(name = "idx_admin_sessions_admin_id", columnList = "admin_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSession extends BaseEntity {

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
