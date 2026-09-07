package com.example.urlshortener.repository;

import java.util.Optional;

import com.example.urlshortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByIdempotencyKey(String idempotencyKey);

    Optional<UrlMapping> findByShortCode(String shortCode);
}
//We use Optional because the data may or may not exist in the database. Instead of returning null, it returns an Optional,
// which makes us handle the "not found" case safely and helps avoid NullPointerException.
//