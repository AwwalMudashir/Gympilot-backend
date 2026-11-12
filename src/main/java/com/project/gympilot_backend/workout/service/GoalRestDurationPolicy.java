package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.profile.model.FitnessGoal;
import java.util.Objects;

final class GoalRestDurationPolicy {
    static final int MIN_REST_SECONDS = 30;
    static final int MAX_REST_SECONDS = 60;

    private GoalRestDurationPolicy() {
    }

    static int secondsFor(FitnessGoal goal) {
        Objects.requireNonNull(goal, "Fitness goal is required to determine rest duration");

        return switch (goal) {
            case WEIGHT_LOSS -> 30;
            case CONSISTENCY -> 35;
            case GENERAL_FITNESS, MAINTENANCE -> 45;
            case MUSCLE_GAIN, STRENGTH -> MAX_REST_SECONDS;
        };
    }
}
