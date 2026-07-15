package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShortenRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must not exceed 2048 characters")
        String url,

        @Size(max = 32, message = "customAlias must not exceed 32 characters")
        String customAlias
) {
}
