package com.example.vnkapp.entity;

import com.example.vnkapp.enums.medication.Frequency;
import com.example.vnkapp.enums.medication.MealRelation;
import com.example.vnkapp.enums.medication.MedicationStatus;
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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_medications", indexes = {
    @Index(name = "idx_user_medications_user_id", columnList = "user_id"),
    @Index(name = "idx_user_medications_family_member_id", columnList = "family_member_id"),
    @Index(name = "idx_user_medications_medication_id", columnList = "medication_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMedication extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_member_id")
    private UUID familyMemberId;

    @Column(name = "medication_id")
    private UUID medicationId;

    @Column(name = "user_disease_id")
    private UUID userDiseaseId;

    @Column(name = "custom_medication_name")
    private String customMedicationName;

    @Column(nullable = false)
    private String dosage;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private Frequency frequency;

    @Column(name = "times_per_day")
    @Builder.Default
    private Integer timesPerDay = 1;

    @Column(name = "specific_times")
    private String specificTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_relation")
    private MealRelation mealRelation;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "prescribed_by")
    private String prescribedBy;

    @Column(name = "prescription_image_url")
    private String prescriptionImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "medication_status", nullable = false)
    @Builder.Default
    private MedicationStatus medicationStatus = MedicationStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reminder_enabled", nullable = false)
    @Builder.Default
    private Boolean reminderEnabled = true;
}
