-- NQIYA initial schema

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    display_name  VARCHAR(40)  NOT NULL,
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER',
    xp            INTEGER      NOT NULL DEFAULT 0 CHECK (xp >= 0),
    coins         INTEGER      NOT NULL DEFAULT 0 CHECK (coins >= 0),
    ticket_code   VARCHAR(12)  NOT NULL UNIQUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_user_xp ON app_user (xp DESC, id);

CREATE TABLE login_code (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(254) NOT NULL,
    code_hash   VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    consumed    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_login_code_email ON login_code (email, created_at DESC);

CREATE TABLE photo (
    id              UUID PRIMARY KEY,
    owner_id        BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    kind            VARCHAR(8)   NOT NULL,
    content_type    VARCHAR(40)  NOT NULL,
    data            BYTEA        NOT NULL,
    ai_accepted     BOOLEAN      NOT NULL,
    ai_title        VARCHAR(80),
    ai_description  VARCHAR(400),
    dirt_level      VARCHAR(8),
    waste_type      VARCHAR(60),
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE spot (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(80)      NOT NULL,
    description         VARCHAR(400),
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    status              VARCHAR(12)      NOT NULL,
    visibility          VARCHAR(8)       NOT NULL,
    anonymous           BOOLEAN          NOT NULL DEFAULT FALSE,
    closed              BOOLEAN          NOT NULL DEFAULT FALSE,
    meeting_at          TIMESTAMPTZ,
    organizer_id        BIGINT           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    before_photo_id     UUID             NOT NULL REFERENCES photo (id),
    after_photo_id      UUID             REFERENCES photo (id),
    dirt_level          VARCHAR(8),
    waste_type          VARCHAR(60),
    participant_count   INTEGER          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT now(),
    started_at          TIMESTAMPTZ,
    cleaned_at          TIMESTAMPTZ,
    version             BIGINT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_spot_geo ON spot (latitude, longitude);
CREATE INDEX idx_spot_status ON spot (status) WHERE closed = FALSE;

CREATE TABLE participation (
    id             BIGSERIAL PRIMARY KEY,
    spot_id        BIGINT      NOT NULL REFERENCES spot (id) ON DELETE CASCADE,
    user_id        BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    joined_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked_in_at  TIMESTAMPTZ,
    CONSTRAINT uq_participation UNIQUE (spot_id, user_id)
);
CREATE INDEX idx_participation_user ON participation (user_id);

CREATE TABLE reward_event (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    type         VARCHAR(20)  NOT NULL,
    xp_delta     INTEGER      NOT NULL DEFAULT 0,
    coins_delta  INTEGER      NOT NULL DEFAULT 0,
    spot_id      BIGINT       REFERENCES spot (id) ON DELETE SET NULL,
    description  VARCHAR(200) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_reward_user ON reward_event (user_id, created_at DESC);

CREATE TABLE voucher (
    id            BIGSERIAL PRIMARY KEY,
    partner_name  VARCHAR(60)  NOT NULL,
    title         VARCHAR(80)  NOT NULL,
    description   VARCHAR(300) NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    cost_coins    INTEGER      NOT NULL CHECK (cost_coins > 0),
    city          VARCHAR(60)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE purchase (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    voucher_id    BIGINT      NOT NULL REFERENCES voucher (id),
    code          VARCHAR(16) NOT NULL UNIQUE,
    status        VARCHAR(8)  NOT NULL,
    cost_coins    INTEGER     NOT NULL,
    purchased_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    used_at       TIMESTAMPTZ
);
CREATE INDEX idx_purchase_user ON purchase (user_id, purchased_at DESC);

CREATE TABLE admin_action (
    id           BIGSERIAL PRIMARY KEY,
    admin_id     BIGINT       REFERENCES app_user (id) ON DELETE SET NULL,
    admin_email  VARCHAR(254) NOT NULL,
    action       VARCHAR(30)  NOT NULL,
    target_type  VARCHAR(20)  NOT NULL,
    target_id    BIGINT       NOT NULL,
    details      VARCHAR(300),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_action_created ON admin_action (created_at DESC);
