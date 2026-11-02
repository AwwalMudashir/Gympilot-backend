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
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_rating", length = 30)
    private DifficultyRating difficultyRating;
}
