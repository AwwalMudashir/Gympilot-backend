ALTER TABLE coach_briefs
    ADD COLUMN IF NOT EXISTS feedback VARCHAR(20),
    ADD COLUMN IF NOT EXISTS feedback_at TIMESTAMPTZ;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_coach_briefs_feedback'
          AND conrelid = 'coach_briefs'::regclass
    ) THEN
        ALTER TABLE coach_briefs
            ADD CONSTRAINT ck_coach_briefs_feedback
                CHECK (feedback IS NULL OR feedback IN ('HELPFUL', 'NOT_USEFUL'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_coach_briefs_user_feedback
    ON coach_briefs(user_id, feedback);
