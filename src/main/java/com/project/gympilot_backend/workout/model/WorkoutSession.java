package com.project.gympilot_backend.workout.model;

import com.project.gympilot_backend.common.model.BaseEntity;
import com.project.gympilot_backend.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "workout_sessions")
public class WorkoutSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id")
    private WorkoutPlan workoutPlan;
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;
    @Column(name = "workout_day")
    private Integer workoutDay;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "active_since")
    private Instant activeSince;
    @Column(name = "current_exercise_index", nullable = false)
    @Builder.Default
    private Integer currentExerciseIndex = 0;
    @Column(name = "current_set_number", nullable = false)
    @Builder.Default
    private Integer currentSetNumber = 1;
    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", nullable = false, length = 30)
    @Builder.Default
    private WorkoutPhase currentPhase = WorkoutPhase.WARM_UP;
    @Column(name = "phase_remaining_seconds", nullable = false)
    @Builder.Default
    private Integer phaseRemainingSeconds = 45;
    @Column(name = "elapsed_seconds", nullable = false)
    @Builder.Default
    private Integer elapsedSeconds = 0;
    @Column(name = "calories_burned", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal caloriesBurned = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_rating", length = 30)
    private DifficultyRating difficultyRating;
    @Column(length = 500)
    private String notes;
}
