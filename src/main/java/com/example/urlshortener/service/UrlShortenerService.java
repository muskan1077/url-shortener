package com.example.urlshortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.InvalidAliasException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.repository.UrlMappingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlShortenerService {

    private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private final UrlMappingRepository repository;

    public UrlShortenerService(UrlMappingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        var originalUrl = validateUrl(request.url());
        var alias = normalizeAlias(request.customAlias());

        if (alias != null) {
            return createMapping(originalUrl, alias, true);
        }

        // TODO: Decide how to make generated short URLs idempotent for repeated URLs.
        // TODO: Replace UUID-based codes with a stronger collision-free strategy.
        return createMapping(originalUrl, generateCode(), false);
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String code) {
        return repository.findByShortCode(code)
                .map(UrlMapping::getOriginalUrl)
                .orElseThrow(() -> new ShortCodeNotFoundException(code));
    }

    private ShortenResponse createMapping(String originalUrl, String shortCode, boolean customAlias) {
        try {
            var mapping = repository.save(new UrlMapping(originalUrl, shortCode, customAlias));
            return toResponse(mapping);
        } catch (DataIntegrityViolationException exception) {
            if (customAlias) {
                throw new AliasAlreadyExistsException(shortCode);
            }
            throw exception;
        }
    }

    private String validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("url is required");
        }
        if (url.length() > 2048) {
            throw new InvalidUrlException("url must not exceed 2048 characters");
        }

        try {
            var uri = new URI(url.trim());
            var scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new InvalidUrlException("url scheme must be http or https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("url must include a valid host");
            }
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException("url must be a valid absolute HTTP or HTTPS URL");
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
        return normalizedAlias;
    }

    private String generateCode() {
        var code = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        while (repository.findByShortCode(code).isPresent()) {
            code = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        return code;
    }

    private ShortenResponse toResponse(UrlMapping mapping) {
        return new ShortenResponse(mapping.getShortCode(), "http://localhost:8080/" + mapping.getShortCode(), mapping.getOriginalUrl());
    }
}
