CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    user_agent VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_auth_sessions_refresh_hash ON auth_sessions (refresh_token_hash);
CREATE INDEX ix_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX ix_auth_sessions_expires_at ON auth_sessions (expires_at);
CREATE INDEX ix_auth_sessions_active_user ON auth_sessions (user_id, revoked_at) WHERE revoked_at IS NULL;

