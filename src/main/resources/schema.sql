CREATE TABLE IF NOT EXISTS url_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_url VARCHAR(2048) NOT NULL,
    short_code VARCHAR(64) NOT NULL,
    custom_alias BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_url_mappings_short_code UNIQUE (short_code)
);
