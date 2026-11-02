CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    profile_picture_url VARCHAR(2048),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_app_users_email UNIQUE (email)
);

CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    age INTEGER,
    height_cm NUMERIC(5,2),
    weight_kg NUMERIC(5,2),
    experience_level VARCHAR(30),
    fitness_goal VARCHAR(40),
    preferred_workout_location VARCHAR(80),
    days_per_week INTEGER,
    workout_duration_minutes INTEGER,
    daily_water_target_ml INTEGER,
    preferred_bedtime TIME,
    preferred_wake_time TIME,
    sleep_target_minutes INTEGER,
    waist_cm NUMERIC(5,2),
    chest_cm NUMERIC(5,2),
    arm_cm NUMERIC(5,2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE user_profile_equipment (
    profile_id UUID NOT NULL,
    equipment VARCHAR(100) NOT NULL,
    PRIMARY KEY (profile_id, equipment),
    CONSTRAINT fk_profile_equipment_profile FOREIGN KEY (profile_id) REFERENCES user_profiles(id) ON DELETE CASCADE
);

CREATE TABLE user_profile_workout_days (
    profile_id UUID NOT NULL,
    workout_day VARCHAR(15) NOT NULL,
    PRIMARY KEY (profile_id, workout_day),
    CONSTRAINT fk_profile_days_profile FOREIGN KEY (profile_id) REFERENCES user_profiles(id) ON DELETE CASCADE
);

CREATE TABLE exercises (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    instructions TEXT,
    muscle_group VARCHAR(80),
    equipment VARCHAR(100),
    camera_supported BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_exercises_code UNIQUE (code)
);

CREATE TABLE workout_plans (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    week_start_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_workout_plans_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE workout_plan_items (
    id UUID PRIMARY KEY,
    workout_plan_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    day_of_week INTEGER NOT NULL,
    sets INTEGER NOT NULL,
    target_repetitions INTEGER NOT NULL,
    rest_seconds INTEGER NOT NULL,
    item_order INTEGER NOT NULL,
    camera_supported BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_plan_items_plan FOREIGN KEY (workout_plan_id) REFERENCES workout_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_items_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id)
);

CREATE TABLE workout_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    workout_plan_id UUID,
    scheduled_date DATE,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    difficulty_rating VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_workout_sessions_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_workout_sessions_plan FOREIGN KEY (workout_plan_id) REFERENCES workout_plans(id) ON DELETE SET NULL
);

CREATE TABLE exercise_results (
    id UUID PRIMARY KEY,
    workout_session_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    planned_sets INTEGER,
    completed_sets INTEGER,
    planned_repetitions INTEGER,
    completed_repetitions INTEGER,
    average_rep_duration_seconds NUMERIC(6,2),
    range_of_motion_score NUMERIC(5,2),
    form_score NUMERIC(5,2),
    form_warnings TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_exercise_results_session FOREIGN KEY (workout_session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_exercise_results_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id)
);

CREATE TABLE sleep_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    slept_at TIMESTAMPTZ NOT NULL,
    woke_at TIMESTAMPTZ NOT NULL,
    quality VARCHAR(20) NOT NULL,
    energy_level INTEGER NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_sleep_logs_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE water_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    amount_ml INTEGER NOT NULL,
    logged_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_water_logs_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE nutrition_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    meal_name VARCHAR(160) NOT NULL,
    calories INTEGER NOT NULL,
    protein_grams NUMERIC(7,2) NOT NULL,
    carbohydrate_grams NUMERIC(7,2) NOT NULL,
    fat_grams NUMERIC(7,2) NOT NULL,
    fibre_grams NUMERIC(7,2),
    logged_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_nutrition_logs_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    workout_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    workout_time TIME,
    hydration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    hydration_interval_minutes INTEGER,
    bedtime_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    bedtime_reminder_time TIME,
    sleep_checkin_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sleep_checkin_time TIME,
    all_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    device_token VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_notification_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE coach_briefs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    brief_type VARCHAR(20) NOT NULL,
    summary TEXT NOT NULL,
    focus_today TEXT,
    cautions TEXT,
    context_snapshot TEXT,
    generated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_coach_briefs_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_by_ip VARCHAR(64),
    user_agent VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE account_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_account_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_workout_plans_user_week ON workout_plans(user_id, week_start_date);
CREATE INDEX idx_workout_sessions_user_date ON workout_sessions(user_id, scheduled_date);
CREATE INDEX idx_sleep_logs_user_time ON sleep_logs(user_id, slept_at);
CREATE INDEX idx_water_logs_user_time ON water_logs(user_id, logged_at);
CREATE INDEX idx_nutrition_logs_user_time ON nutrition_logs(user_id, logged_at);
CREATE INDEX idx_coach_briefs_user_time ON coach_briefs(user_id, generated_at);
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens(user_id, revoked_at, expires_at);
CREATE INDEX idx_account_tokens_user_purpose ON account_tokens(user_id, purpose, consumed_at);
