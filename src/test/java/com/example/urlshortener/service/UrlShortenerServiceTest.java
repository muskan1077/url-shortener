package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.InvalidAliasException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UrlShortenerServiceTest {

    @Autowired
    private UrlShortenerService service;

    @Autowired
    private UrlMappingRepository repository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Test
    void returnsSameGeneratedCodeForSameUrl() {
        var first = service.shorten(new ShortenRequest("https://example.com/article?id=10", null));
        var second = service.shorten(new ShortenRequest(" HTTPS://Example.COM:443/article?id=10#ignored ", null));

        assertThat(second.shortCode()).isEqualTo(first.shortCode());
        assertThat(second.shortUrl()).isEqualTo(first.shortUrl());
        assertThat(first.shortCode()).startsWith("u_");
    }

    @Test
    void createsCustomAlias() {
        var response = service.shorten(new ShortenRequest("https://example.com/docs", "docs-2026"));

        assertThat(response.shortCode()).isEqualTo("docs-2026");
        assertThat(response.shortUrl()).isEqualTo("http://sho.rt/docs-2026");
        assertThat(response.originalUrl()).isEqualTo("https://example.com/docs");
    }

    @Test
    void allowsDifferentCustomAliasesForSameUrl() {
        var first = service.shorten(new ShortenRequest("https://example.com/same", "same-one"));
        var second = service.shorten(new ShortenRequest("https://example.com/same", "same-two"));

        assertThat(first.shortCode()).isEqualTo("same-one");
        assertThat(second.shortCode()).isEqualTo("same-two");
    }

    @Test
    void rejectsDuplicateCustomAlias() {
        service.shorten(new ShortenRequest("https://example.com/one", "taken"));

        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com/two", "taken")))
                .isInstanceOf(AliasAlreadyExistsException.class)
                .hasMessage("Alias already exists: taken");
    }

    @Test
    void rejectsReservedAliasPrefix() {
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com", "u_abc")))
                .isInstanceOf(InvalidAliasException.class)
                .hasMessage("customAlias must not start with reserved prefix u_");
    }

    @Test
    void rejectsInvalidAliasCharacters() {
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com", "bad alias")))
                .isInstanceOf(InvalidAliasException.class);
    }

    @Test
    void rejectsInvalidUrl() {
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("javascript:alert(1)", null)))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void findsOriginalUrlByCode() {
        var response = service.shorten(new ShortenRequest("https://example.com/redirect", "go-here"));

        assertThat(service.getOriginalUrl(response.shortCode()))
                .isEqualTo("https://example.com/redirect");
    }

    @Test
    void throwsWhenCodeDoesNotExist() {
        assertThatThrownBy(() -> service.getOriginalUrl("missing"))
                .isInstanceOf(ShortCodeNotFoundException.class)
                .hasMessage("Short code not found: missing");
    }
}
