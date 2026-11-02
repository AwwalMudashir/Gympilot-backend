package com.project.gympilot_backend.workout.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "exercises")
public class Exercise extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String instructions;
    @Column(name = "muscle_group", length = 80)
    private String muscleGroup;
    @Column(length = 100)
    private String equipment;
    @Column(name = "camera_supported", nullable = false)
    private boolean cameraSupported;
    @Column(nullable = false)
    private boolean active;
}
