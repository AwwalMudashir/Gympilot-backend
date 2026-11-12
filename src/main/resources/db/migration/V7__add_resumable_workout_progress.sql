ALTER TABLE workout_sessions
    ADD COLUMN current_exercise_index INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN current_set_number INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN current_phase VARCHAR(30) NOT NULL DEFAULT 'WARM_UP',
    ADD COLUMN phase_remaining_seconds INTEGER NOT NULL DEFAULT 45,
    ADD COLUMN elapsed_seconds INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN active_since TIMESTAMPTZ;

UPDATE workout_sessions
SET active_since = started_at
WHERE status = 'IN_PROGRESS'
  AND started_at IS NOT NULL;

ALTER TABLE workout_sessions
    ADD CONSTRAINT chk_workout_session_exercise_index
        CHECK (current_exercise_index >= 0),
    ADD CONSTRAINT chk_workout_session_set_number
        CHECK (current_set_number >= 1),
    ADD CONSTRAINT chk_workout_session_phase_remaining
        CHECK (phase_remaining_seconds >= 0),
    ADD CONSTRAINT chk_workout_session_elapsed
        CHECK (elapsed_seconds >= 0);

CREATE UNIQUE INDEX uq_exercise_results_session_exercise
    ON exercise_results(workout_session_id, exercise_id);
