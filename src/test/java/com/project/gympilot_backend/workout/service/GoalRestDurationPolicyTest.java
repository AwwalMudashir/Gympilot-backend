package com.project.gympilot_backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.gympilot_backend.profile.model.FitnessGoal;
import org.junit.jupiter.api.Test;

class GoalRestDurationPolicyTest {

    @Test
    void assignsSafeRestPeriodsForEveryFitnessGoal() {
        assertThat(GoalRestDurationPolicy.secondsFor(FitnessGoal.WEIGHT_LOSS)).isEqualTo(30);
        assertThat(GoalRestDurationPolicy.secondsFor(FitnessGoal.CONSISTENCY)).isEqualTo(35);
        assertThat(GoalRestDurationPolicy.secondsFor(FitnessGoal.GENERAL_FITNESS)).isEqualTo(45);
        assertThat(GoalRestDurationPolicy.secondsFor(FitnessGoal.MAINTENANCE)).isEqualTo(45);
        assertThat(GoalRestDurationPolicy.secondsFor(FitnessGoal.MUSCLE_GAIN)).isEqualTo(60);
        assertThat(GoalRestDurationPolicy.secondsFor(FitnessGoal.STRENGTH)).isEqualTo(60);
    }

    @Test
    void neverExceedsTheConfiguredSixtySecondMaximum() {
        assertThat(FitnessGoal.values())
                .allSatisfy(goal -> assertThat(GoalRestDurationPolicy.secondsFor(goal))
                        .isBetween(
                                GoalRestDurationPolicy.MIN_REST_SECONDS,
                                GoalRestDurationPolicy.MAX_REST_SECONDS));
    }
}
