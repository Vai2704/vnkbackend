package com.example.vnkapp.entity;

import com.example.vnkapp.enums.medication.MedicationForm;
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

@Entity
@Table(name = "medications", indexes = {
    @Index(name = "idx_medications_name", columnList = "name"),
    @Index(name = "idx_medications_code", columnList = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    @Column(unique = true)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "form")
    private MedicationForm form;

    @Column(name = "strength")
    private String strength;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Column(name = "precautions", columnDefinition = "TEXT")
    private String precautions;

    @Column(name = "storage_instructions")
    private String storageInstructions;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_prescription_required", nullable = false)
    @Builder.Default
    private Boolean isPrescriptionRequired = false;
}
