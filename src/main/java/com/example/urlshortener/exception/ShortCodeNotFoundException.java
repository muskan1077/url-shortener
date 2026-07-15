package com.example.urlshortener.exception;

public class ShortCodeNotFoundException extends RuntimeException {

    public ShortCodeNotFoundException(String code) {
        super("Short code not found: " + code);
    }
}
