CREATE TABLE IF NOT EXISTS url_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_url VARCHAR(2048) NOT NULL,
    original_url_hash CHAR(64) NOT NULL,
    idempotency_key CHAR(64) NULL,
    short_code VARCHAR(64) NOT NULL,
    custom_alias BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_url_mappings_short_code UNIQUE (short_code),
    CONSTRAINT uk_url_mappings_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_url_mappings_original_url_hash (original_url_hash)
);
