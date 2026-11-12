UPDATE workout_plan_items AS item
SET rest_seconds = GREATEST(
    30,
    LEAST(
        item.rest_seconds,
        CASE profile.fitness_goal
            WHEN 'WEIGHT_LOSS' THEN 30
            WHEN 'CONSISTENCY' THEN 35
            WHEN 'GENERAL_FITNESS' THEN 45
            WHEN 'MAINTENANCE' THEN 45
            WHEN 'MUSCLE_GAIN' THEN 60
            WHEN 'STRENGTH' THEN 60
            ELSE 45
        END
    )
)
FROM workout_plans AS plan
JOIN user_profiles AS profile ON profile.user_id = plan.user_id
WHERE item.workout_plan_id = plan.id;

ALTER TABLE workout_plan_items
    ADD CONSTRAINT chk_workout_plan_item_rest_seconds
        CHECK (rest_seconds BETWEEN 30 AND 60);
