package com.example.urlshortener.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void shortenReturnsCreatedResponse() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/api"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", startsWith("u_")))
                .andExpect(jsonPath("$.shortUrl", startsWith("http://sho.rt/u_")))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/api"));
    }

    @Test
    void shortenRejectsInvalidUrl() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "notaurl"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("url must include http or https scheme"));
    }

    @Test
    void shortenRejectsDuplicateAlias() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/first",
                                  "customAlias": "dupe"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/second",
                                  "customAlias": "dupe"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Alias already exists: dupe"));
    }

    @Test
    void redirectReturnsMovedPermanently() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/landing",
                                  "customAlias": "landing"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/landing"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://example.com/landing"));
    }

    @Test
    void redirectReturnsNotFoundForMissingCode() throws Exception {
        mockMvc.perform(get("/missing-code"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short code not found: missing-code"));
    }
}
