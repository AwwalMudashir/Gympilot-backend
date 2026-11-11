package com.project.gympilot_backend.workout.service;

import com.project.gympilot_backend.workout.model.WorkoutPlanItem;
import java.util.List;

public final class WorkoutTimingCalculator {
    public static final int WARM_UP_SECONDS = 5 * 60;
    public static final int REPETITION_SECONDS = 3;
    public static final int EXERCISE_TRANSITION_SECONDS = 45;

    private WorkoutTimingCalculator() {
    }

    public static int estimateSeconds(
            int exerciseCount,
            int sets,
            int repetitions,
            int restSeconds
    ) {
        int movementSeconds = exerciseCount * sets * repetitions * REPETITION_SECONDS;
        int betweenSetRestSeconds = exerciseCount * Math.max(0, sets - 1) * restSeconds;
        int transitionSeconds = Math.max(0, exerciseCount - 1) * EXERCISE_TRANSITION_SECONDS;
        return WARM_UP_SECONDS + movementSeconds + betweenSetRestSeconds + transitionSeconds;
    }

    public static int estimateSeconds(List<WorkoutPlanItem> items) {
        if (items.isEmpty()) return 0;
        int movementSeconds = items.stream()
                .mapToInt(item -> item.getSets() * item.getTargetRepetitions() * REPETITION_SECONDS)
                .sum();
        int betweenSetRestSeconds = items.stream()
                .mapToInt(item -> Math.max(0, item.getSets() - 1) * item.getRestSeconds())
                .sum();
        int transitionSeconds = Math.max(0, items.size() - 1) * EXERCISE_TRANSITION_SECONDS;
        return WARM_UP_SECONDS + movementSeconds + betweenSetRestSeconds + transitionSeconds;
    }

    public static int estimateMinutes(List<WorkoutPlanItem> items) {
        return (int) Math.ceil(estimateSeconds(items) / 60.0);
    }
}
