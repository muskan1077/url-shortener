package com.example.urlshortener.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String shortBaseUrl,
        Cache cache
) {

    public AppProperties {
        if (shortBaseUrl == null || shortBaseUrl.isBlank()) {
            shortBaseUrl = "http://localhost:8080";
        }
        if (cache == null) {
            cache = new Cache(Duration.ofMinutes(10));
        }
    }

    public record Cache(Duration ttl) {
        public Cache {
            if (ttl == null || ttl.isNegative() || ttl.isZero()) {
                ttl = Duration.ofMinutes(10);
            }
        }
    }
}
