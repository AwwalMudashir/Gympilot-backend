DELETE FROM exercise_results older
USING exercise_results newer
WHERE older.workout_session_id = newer.workout_session_id
  AND older.exercise_id = newer.exercise_id
  AND (older.updated_at, older.id) < (newer.updated_at, newer.id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exercise_results_session_exercise
    ON exercise_results(workout_session_id, exercise_id);
