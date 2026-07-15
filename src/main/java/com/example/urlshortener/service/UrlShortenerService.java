package com.example.urlshortener.service;

import java.util.UUID;
import java.util.regex.Pattern;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.config.CacheConfig;
import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.InvalidAliasException;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62Encoder;
import com.example.urlshortener.util.UrlHasher;
import com.example.urlshortener.util.UrlNormalizer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UrlShortenerService {

    private static final String GENERATED_CODE_PREFIX = "u_";
    private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private final UrlMappingRepository repository;
    private final UrlNormalizer urlNormalizer;
    private final UrlHasher urlHasher;
    private final Base62Encoder base62Encoder;
    private final TransactionTemplate transactionTemplate;
    private final String shortBaseUrl;

    public UrlShortenerService(
            UrlMappingRepository repository,
            UrlNormalizer urlNormalizer,
            UrlHasher urlHasher,
            Base62Encoder base62Encoder,
            TransactionTemplate transactionTemplate,
            AppProperties appProperties
    ) {
        this.repository = repository;
        this.urlNormalizer = urlNormalizer;
        this.urlHasher = urlHasher;
        this.base62Encoder = base62Encoder;
        this.transactionTemplate = transactionTemplate;
        this.shortBaseUrl = trimTrailingSlash(appProperties.shortBaseUrl());
    }

    public ShortenResponse shorten(ShortenRequest request) {
        var normalizedUrl = urlNormalizer.normalize(request.url());
        var urlHash = urlHasher.sha256Hex(normalizedUrl);
        var alias = normalizeAlias(request.customAlias());

        if (alias != null) {
            return createCustomAlias(normalizedUrl, urlHash, alias);
        }

        return createOrGetGeneratedCode(normalizedUrl, urlHash);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.URL_BY_CODE_CACHE, key = "#code")
    public String getOriginalUrl(String code) {
        return repository.findByShortCode(code)
                .map(UrlMapping::getOriginalUrl)
                .orElseThrow(() -> new ShortCodeNotFoundException(code));
    }

    private ShortenResponse createOrGetGeneratedCode(String normalizedUrl, String urlHash) {
        var existing = repository.findByIdempotencyKey(urlHash);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        try {
            var mapping = transactionTemplate.execute(status -> {
                var pendingCode = "pending_" + UUID.randomUUID();
                var saved = repository.saveAndFlush(new UrlMapping(normalizedUrl, urlHash, urlHash, pendingCode, false));
                saved.setShortCode(GENERATED_CODE_PREFIX + base62Encoder.encode(saved.getId()));
                return saved;
            });
            return toResponse(mapping);
        } catch (DataIntegrityViolationException exception) {
            return repository.findByIdempotencyKey(urlHash)
                    .map(this::toResponse)
                    .orElseThrow(() -> exception);
        }
    }

    private ShortenResponse createCustomAlias(String normalizedUrl, String urlHash, String alias) {
        try {
            var mapping = transactionTemplate.execute(status ->
                    repository.save(new UrlMapping(normalizedUrl, urlHash, null, alias, true))
            );
            return toResponse(mapping);
        } catch (DataIntegrityViolationException exception) {
            throw new AliasAlreadyExistsException(alias);
        }
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }

        var normalizedAlias = alias.trim();
        if (!CUSTOM_ALIAS_PATTERN.matcher(normalizedAlias).matches()) {
            throw new InvalidAliasException("customAlias must be 3-32 characters and contain only letters, numbers, hyphen, or underscore");
        }
        if (normalizedAlias.startsWith(GENERATED_CODE_PREFIX)) {
            throw new InvalidAliasException("customAlias must not start with reserved prefix " + GENERATED_CODE_PREFIX);
        }
        return normalizedAlias;
    }

    private ShortenResponse toResponse(UrlMapping mapping) {
        return new ShortenResponse(
                mapping.getShortCode(),
                shortBaseUrl + "/" + mapping.getShortCode(),
                mapping.getOriginalUrl()
        );
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
