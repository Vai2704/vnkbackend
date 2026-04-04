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

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "medication_reminders", indexes = {
    @Index(name = "idx_medication_reminders_user_medication_id", columnList = "user_medication_id"),
    @Index(name = "idx_medication_reminders_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationReminder extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_medication_id", nullable = false)
    private UUID userMedicationId;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    @Column(name = "reminder_label")
    private String reminderLabel;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "snooze_minutes")
    @Builder.Default
    private Integer snoozeMinutes = 10;

    @Column(name = "repeat_until_taken", nullable = false)
    @Builder.Default
    private Boolean repeatUntilTaken = true;

    @Column(name = "max_repeat_count")
    @Builder.Default
    private Integer maxRepeatCount = 3;
}
