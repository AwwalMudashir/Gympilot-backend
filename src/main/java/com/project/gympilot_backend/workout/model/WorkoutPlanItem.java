package com.project.gympilot_backend.workout.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "workout_plan_items")
public class WorkoutPlanItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    private WorkoutPlan workoutPlan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;
    @Column(nullable = false)
    private int sets;
    @Column(name = "target_repetitions", nullable = false)
    private int targetRepetitions;
    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;
    @Column(name = "item_order", nullable = false)
    private int itemOrder;
    @Column(name = "camera_supported", nullable = false)
    private boolean cameraSupported;
}
