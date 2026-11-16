ALTER TABLE exercises
    ADD COLUMN met_value NUMERIC(4,2) NOT NULL DEFAULT 4.00;

ALTER TABLE exercises
    ADD CONSTRAINT chk_exercises_met_value
        CHECK (met_value >= 1.00 AND met_value <= 20.00);

ALTER TABLE exercise_results
    ADD COLUMN calories_burned NUMERIC(8,2) NOT NULL DEFAULT 0.00;

ALTER TABLE workout_sessions
    ADD COLUMN calories_burned NUMERIC(8,2) NOT NULL DEFAULT 0.00;

ALTER TABLE exercise_results
    ADD CONSTRAINT chk_exercise_results_calories
        CHECK (calories_burned >= 0.00);

ALTER TABLE workout_sessions
    ADD CONSTRAINT chk_workout_sessions_calories
        CHECK (calories_burned >= 0.00);

UPDATE exercises SET met_value = 5.00 WHERE code = 'BODYWEIGHT_SQUAT';
UPDATE exercises SET met_value = 3.50 WHERE code = 'BICEP_CURL';
UPDATE exercises SET met_value = 4.00 WHERE code = 'PUSH_UP';
UPDATE exercises SET met_value = 3.80 WHERE code = 'GLUTE_BRIDGE';
UPDATE exercises SET met_value = 5.00 WHERE code = 'REVERSE_LUNGE';
UPDATE exercises SET met_value = 3.30 WHERE code = 'PLANK';
UPDATE exercises SET met_value = 3.00 WHERE code = 'DEAD_BUG';
UPDATE exercises SET met_value = 8.00 WHERE code = 'MOUNTAIN_CLIMBER';
UPDATE exercises SET met_value = 5.00 WHERE code = 'DUMBBELL_ROW';
UPDATE exercises SET met_value = 5.00 WHERE code = 'SHOULDER_PRESS';
UPDATE exercises SET met_value = 3.50 WHERE code = 'BAND_PULL_APART';
UPDATE exercises SET met_value = 4.00 WHERE code = 'BANDED_GLUTE_WALK';
UPDATE exercises SET met_value = 4.50 WHERE code = 'BENCH_DIP';
UPDATE exercises SET met_value = 5.50 WHERE code = 'BENCH_STEP_UP';
UPDATE exercises SET met_value = 5.00 WHERE code = 'KETTLEBELL_GOBLET_SQUAT';
UPDATE exercises SET met_value = 5.50 WHERE code = 'KETTLEBELL_DEADLIFT';
UPDATE exercises SET met_value = 6.00 WHERE code = 'BARBELL_ROMANIAN_DEADLIFT';
UPDATE exercises SET met_value = 6.00 WHERE code = 'BARBELL_BENT_ROW';
UPDATE exercises SET met_value = 8.00 WHERE code = 'MEDICINE_BALL_SLAM';
UPDATE exercises SET met_value = 6.00 WHERE code = 'ASSISTED_PULL_UP';
UPDATE exercises SET met_value = 5.00 WHERE code = 'HANGING_KNEE_RAISE';
UPDATE exercises SET met_value = 5.00 WHERE code = 'CABLE_CHEST_PRESS';
UPDATE exercises SET met_value = 5.00 WHERE code = 'CABLE_LAT_PULLDOWN';
UPDATE exercises SET met_value = 5.50 WHERE code = 'SMITH_MACHINE_SQUAT';
UPDATE exercises SET met_value = 5.00 WHERE code = 'LEG_PRESS';
UPDATE exercises SET met_value = 4.00 WHERE code = 'STABILITY_BALL_LEG_CURL';
UPDATE exercises SET met_value = 5.00 WHERE code = 'SUSPENSION_ROW';
UPDATE exercises SET met_value = 6.00 WHERE code = 'PLYO_BOX_STEP_UP';
UPDATE exercises SET met_value = 5.00 WHERE code = 'AB_WHEEL_ROLLOUT';

-- Preserve useful analytics for workouts completed before this migration. The
-- same MET formula used by the application is applied to historical results.
UPDATE exercise_results er
SET calories_burned = ROUND(
        (
            e.met_value
            * 3.5
            * COALESCE(up.weight_kg, 70)
            / 200
        )
        * (
            COALESCE(er.completed_sets, 0)
            * COALESCE(er.completed_repetitions, 0)
            * 3
            / 60.0
        ),
        2
    )
FROM exercises e,
     workout_sessions ws,
     user_profiles up
WHERE er.exercise_id = e.id
  AND er.workout_session_id = ws.id
  AND ws.user_id = up.user_id
  AND COALESCE(er.skipped, FALSE) = FALSE;

UPDATE workout_sessions ws
SET calories_burned = totals.calories_burned
FROM (
    SELECT workout_session_id,
           COALESCE(SUM(calories_burned), 0) AS calories_burned
    FROM exercise_results
    GROUP BY workout_session_id
) totals
WHERE ws.id = totals.workout_session_id;
