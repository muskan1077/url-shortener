package com.example.urlshortener.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encodesKnownValues() {
        assertThat(encoder.encode(0)).isEqualTo("0");
        assertThat(encoder.encode(1)).isEqualTo("1");
        assertThat(encoder.encode(61)).isEqualTo("z");
        assertThat(encoder.encode(62)).isEqualTo("10");
        assertThat(encoder.encode(3843)).isEqualTo("zz");
    }

    @Test
    void rejectsNegativeNumbers() {
        assertThatThrownBy(() -> encoder.encode(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be non-negative");
    }
}
