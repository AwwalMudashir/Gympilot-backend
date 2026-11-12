package com.project.gympilot_backend.vision.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import com.project.gympilot_backend.workout.model.Exercise;
import com.project.gympilot_backend.workout.model.WorkoutSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "exercise_results")
public class ExerciseResult extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
    @Column(name = "planned_sets")
    private Integer plannedSets;
    @Column(name = "completed_sets")
    private Integer completedSets;
    @Column(name = "planned_repetitions")
    private Integer plannedRepetitions;
    @Column(name = "completed_repetitions")
    private Integer completedRepetitions;
    @Builder.Default
    @Column(name = "skipped", nullable = false)
    private boolean skipped = false;
    @Column(name = "average_rep_duration_seconds", precision = 6, scale = 2)
    private BigDecimal averageRepDurationSeconds;
    @Column(name = "range_of_motion_score", precision = 5, scale = 2)
    private BigDecimal rangeOfMotionScore;
    @Column(name = "form_score", precision = 5, scale = 2)
    private BigDecimal formScore;
    @Column(name = "form_warnings", columnDefinition = "TEXT")
    private String formWarnings;
}
