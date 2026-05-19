package com.example.vnkapp.entity;

import com.example.vnkapp.enums.medication.DiseaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_diseases", indexes = {
    @Index(name = "idx_user_diseases_user_id", columnList = "user_id"),
    @Index(name = "idx_user_diseases_family_member_id", columnList = "family_member_id"),
    @Index(name = "idx_user_diseases_disease_id", columnList = "disease_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_diseases", columnNames = {"user_id", "family_member_id", "disease_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDisease extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_member_id")
    private UUID familyMemberId;

    @Column(name = "disease_id", nullable = false)
    private UUID diseaseId;

    @Column(name = "custom_disease_name")
    private String customDiseaseName;

    @Column(name = "diagnosed_date")
    private LocalDate diagnosedDate;

    @Column(name = "diagnosed_by")
    private String diagnosedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "disease_status", nullable = false)
    @Builder.Default
    private DiseaseStatus diseaseStatus = DiseaseStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
