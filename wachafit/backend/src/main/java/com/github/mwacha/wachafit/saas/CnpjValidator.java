package com.github.mwacha.wachafit.saas;

public final class CnpjValidator {

    private static final int[] WEIGHTS_FIRST  = {5,4,3,2,9,8,7,6,5,4,3,2};
    private static final int[] WEIGHTS_SECOND = {6,5,4,3,2,9,8,7,6,5,4,3,2};

    private CnpjValidator() {}

    public static boolean isValid(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}")) return false;
        if (cnpj.chars().distinct().count() == 1) return false;

        int[] digits = cnpj.chars().map(c -> c - '0').toArray();
        if (calculateCheckDigit(digits, WEIGHTS_FIRST) != digits[12]) return false;
        return calculateCheckDigit(digits, WEIGHTS_SECOND) == digits[13];
    }

    private static int calculateCheckDigit(int[] digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += digits[i] * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
