CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    asset_id VARCHAR(255),
    public_id VARCHAR(512) NOT NULL,
    secure_url VARCHAR(2048) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    format VARCHAR(30),
    byte_size BIGINT NOT NULL,
    width INTEGER,
    height INTEGER,
    purpose VARCHAR(40) NOT NULL,
    original_filename VARCHAR(255),
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_media_assets_public_id UNIQUE (public_id),
    CONSTRAINT uq_media_assets_asset_id UNIQUE (asset_id),
    CONSTRAINT fk_media_assets_owner FOREIGN KEY (owner_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_media_assets_owner_purpose
    ON media_assets(owner_id, purpose, deleted_at, created_at);
