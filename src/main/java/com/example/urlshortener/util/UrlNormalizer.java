package com.example.urlshortener.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import com.example.urlshortener.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

@Component
public class UrlNormalizer {

    public String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidUrlException("url is required");
        }

        var trimmed = input.trim();
        if (trimmed.length() > 2048) {
            throw new InvalidUrlException("url must not exceed 2048 characters");
        }

        try {
            var uri = new URI(trimmed).normalize();
            var scheme = validateScheme(uri);
            var host = validateHost(uri);
            var port = normalizePort(scheme, uri.getPort());
            var path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();

            return new URI(
                    scheme,
                    uri.getRawUserInfo(),
                    host,
                    port,
                    path,
                    uri.getRawQuery(),
                    null
            ).toString();
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException("url must be a valid absolute HTTP or HTTPS URL");
        }
    }

    private String validateScheme(URI uri) {
        if (uri.getScheme() == null) {
            throw new InvalidUrlException("url must include http or https scheme");
        }

        var scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidUrlException("url scheme must be http or https");
        }
        return scheme;
    }

    private String validateHost(URI uri) {
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("url must include a valid host");
        }
        return uri.getHost().toLowerCase(Locale.ROOT);
    }

    private int normalizePort(String scheme, int port) {
        if (port == 80 && scheme.equals("http")) {
            return -1;
        }
        if (port == 443 && scheme.equals("https")) {
            return -1;
        }
        return port;
    }
}
