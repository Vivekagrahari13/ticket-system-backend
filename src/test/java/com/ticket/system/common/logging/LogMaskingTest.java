package com.ticket.system.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskingTest {

    @ParameterizedTest
    @CsvSource({
            "jane.doe@company.com, j***@company.com",
            "a@x.com, a***@x.com",
            "@company.com, ***@company.com",
            "not-an-email, ***"
    })
    void maskEmail_hidesTheLocalPartButKeepsTheDomain(String input, String expected) {
        assertThat(LogMasking.maskEmail(input)).isEqualTo(expected);
    }

    @Test
    void maskEmail_handlesNullAndBlank() {
        assertThat(LogMasking.maskEmail(null)).isEmpty();
        assertThat(LogMasking.maskEmail("  ")).isEmpty();
    }
}
