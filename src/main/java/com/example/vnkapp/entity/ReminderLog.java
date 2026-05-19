package com.example.vnkapp.entity;

import com.example.vnkapp.enums.medication.ReminderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reminder_logs", indexes = {
    @Index(name = "idx_reminder_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_reminder_logs_user_medication_id", columnList = "user_medication_id"),
    @Index(name = "idx_reminder_logs_scheduled_date", columnList = "scheduled_date"),
    @Index(name = "idx_reminder_logs_status", columnList = "reminder_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_medication_id", nullable = false)
    private UUID userMedicationId;

    @Column(name = "medication_reminder_id", nullable = false)
    private UUID medicationReminderId;

    @Column(name = "family_member_id")
    private UUID familyMemberId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_status", nullable = false)
    @Builder.Default
    private ReminderStatus reminderStatus = ReminderStatus.PENDING;

    @Column(name = "taken_at")
    private Instant takenAt;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    @Column(name = "snooze_count")
    @Builder.Default
    private Integer snoozeCount = 0;

    @Column(name = "skipped_at")
    private Instant skippedAt;

    @Column(name = "skip_reason")
    private String skipReason;

    @Column(name = "notes")
    private String notes;
}
