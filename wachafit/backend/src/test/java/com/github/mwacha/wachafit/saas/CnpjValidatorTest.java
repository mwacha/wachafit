package com.github.mwacha.wachafit.saas;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CnpjValidatorTest {

    @Test
    void validCnpj_returnsTrue() {
        assertThat(CnpjValidator.isValid("11222333000181")).isTrue();
    }

    @Test
    void invalidCheckDigit_returnsFalse() {
        assertThat(CnpjValidator.isValid("11222333000180")).isFalse();
    }

    @Test
    void wrongLength_returnsFalse() {
        assertThat(CnpjValidator.isValid("1122233300018")).isFalse();
    }

    @Test
    void allSameDigits_returnsFalse() {
        assertThat(CnpjValidator.isValid("11111111111111")).isFalse();
    }

    @Test
    void nonNumeric_returnsFalse() {
        assertThat(CnpjValidator.isValid("1122233300018a")).isFalse();
    }
}
