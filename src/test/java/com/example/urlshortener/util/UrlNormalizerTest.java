package com.example.urlshortener.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void normalizesSchemeHostDefaultPortAndEmptyPath() {
        assertThat(normalizer.normalize(" HTTPS://Example.COM:443 "))
                .isEqualTo("https://example.com/");
    }

    @Test
    void preservesQueryAndRemovesFragment() {
        assertThat(normalizer.normalize("https://example.com/products?q=phone#details"))
                .isEqualTo("https://example.com/products?q=phone");
    }

    @Test
    void rejectsMissingScheme() {
        assertThatThrownBy(() -> normalizer.normalize("example.com"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessage("url must include http or https scheme");
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThatThrownBy(() -> normalizer.normalize("ftp://example.com/file"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessage("url scheme must be http or https");
    }

    @Test
    void rejectsMissingHost() {
        assertThatThrownBy(() -> normalizer.normalize("https:///missing-host"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessage("url must include a valid host");
    }
}
