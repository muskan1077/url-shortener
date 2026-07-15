package com.example.urlshortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    public String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }
        if (value == 0) {
            return String.valueOf(ALPHABET[0]);
        }

        var encoded = new StringBuilder();
        var remaining = value;
        while (remaining > 0) {
            encoded.append(ALPHABET[(int) (remaining % ALPHABET.length)]);
            remaining = remaining / ALPHABET.length;
        }
        return encoded.reverse().toString();
    }
}
