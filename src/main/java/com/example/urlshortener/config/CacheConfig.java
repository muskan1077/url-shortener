package com.example.urlshortener.config;

import java.util.List;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class CacheConfig {

    public static final String URL_BY_CODE_CACHE = "urlByCode";

    @Bean
    CacheManager cacheManager(AppProperties appProperties) {
        var urlByCodeCache = new CaffeineCache(
                URL_BY_CODE_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(appProperties.cache().ttl())
                        .maximumSize(100_000)
                        .build()
        );

        var cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(urlByCodeCache));
        return cacheManager;
    }
}
