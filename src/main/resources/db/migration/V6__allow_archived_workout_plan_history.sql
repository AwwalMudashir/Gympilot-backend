DROP INDEX IF EXISTS uq_workout_plans_user_week;

CREATE UNIQUE INDEX uq_workout_plans_user_active_week
    ON workout_plans(user_id, week_start_date)
    WHERE status = 'ACTIVE';
