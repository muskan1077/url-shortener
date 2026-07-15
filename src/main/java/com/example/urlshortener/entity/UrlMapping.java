package com.example.urlshortener.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "url_mappings",
        indexes = {
                @Index(name = "idx_url_mappings_original_url_hash", columnList = "original_url_hash")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_url_mappings_short_code", columnNames = "short_code"),
                @UniqueConstraint(name = "uk_url_mappings_idempotency_key", columnNames = "idempotency_key")
        }
)
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "original_url_hash", nullable = false, length = 64)
    private String originalUrlHash;

    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "short_code", nullable = false, length = 64, unique = true)
    private String shortCode;

    @Column(name = "custom_alias", nullable = false)
    private boolean customAlias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UrlMapping() {
    }

    public UrlMapping(String originalUrl, String originalUrlHash, String idempotencyKey, String shortCode, boolean customAlias) {
        this.originalUrl = originalUrl;
        this.originalUrlHash = originalUrlHash;
        this.idempotencyKey = idempotencyKey;
        this.shortCode = shortCode;
        this.customAlias = customAlias;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getOriginalUrlHash() {
        return originalUrlHash;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public boolean isCustomAlias() {
        return customAlias;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
