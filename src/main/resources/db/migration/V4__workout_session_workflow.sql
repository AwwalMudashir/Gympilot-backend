ALTER TABLE workout_sessions
    ADD COLUMN workout_day INTEGER,
    ADD COLUMN notes VARCHAR(500);

ALTER TABLE workout_sessions
    ADD CONSTRAINT chk_workout_sessions_day
        CHECK (workout_day IS NULL OR workout_day BETWEEN 1 AND 7);

CREATE UNIQUE INDEX uq_workout_sessions_plan_day
    ON workout_sessions(user_id, workout_plan_id, workout_day)
    WHERE workout_plan_id IS NOT NULL AND workout_day IS NOT NULL;
