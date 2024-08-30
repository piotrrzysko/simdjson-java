package org.simdjson;

import org.simdjson.ExponentParser.ExponentParsingResult;

import static org.simdjson.CharacterUtils.isStructuralOrWhitespace;
import static org.simdjson.ExponentParser.isExponentIndicator;

class NumberParser {

    private static final int BYTE_MAX_DIGIT_COUNT = 3;
    private static final int BYTE_MAX_ABS_VALUE = 128;
    private static final int SHORT_MAX_DIGIT_COUNT = 5;
    private static final int SHORT_MAX_ABS_VALUE = 32768;
    private static final int INT_MAX_DIGIT_COUNT = 10;
    private static final long INT_MAX_ABS_VALUE = 2147483648L;
    private static final int LONG_MAX_DIGIT_COUNT = 19;

    private final DigitsParsingResult digitsParsingResult = new DigitsParsingResult();
    private final ExponentParser exponentParser = new ExponentParser();
    private final DoubleParser doubleParser = new DoubleParser();
    private final FloatParser floatParser = new FloatParser();

    void parseNumber(byte[] buffer, int offset, Tape tape) {
        boolean negative = buffer[offset] == '-';

        int currentIdx = negative ? offset + 1 : offset;

        int digitsStartIdx = currentIdx;
        DigitsParsingResult digitsParsingResult = parseDigits(buffer, currentIdx, 0);
        long digits = digitsParsingResult.digits();
        currentIdx = digitsParsingResult.currentIdx();
        int digitCount = currentIdx - digitsStartIdx;
        if (digitCount == 0) {
            throw new JsonParsingException("Invalid number. Minus has to be followed by a digit.");
        }
        if ('0' == buffer[digitsStartIdx] && digitCount > 1) {
            throw new JsonParsingException("Invalid number. Leading zeroes are not allowed.");
        }

        long exponent = 0;
        boolean floatingPointNumber = false;
        if ('.' == buffer[currentIdx]) {
            floatingPointNumber = true;
            currentIdx++;
            int firstIdxAfterPeriod = currentIdx;
            digitsParsingResult = parseDigits(buffer, currentIdx, digits);
            digits = digitsParsingResult.digits();
            currentIdx = digitsParsingResult.currentIdx();
            exponent = firstIdxAfterPeriod - currentIdx;
            if (exponent == 0) {
                throw new JsonParsingException("Invalid number. Decimal point has to be followed by a digit.");
            }
            digitCount = currentIdx - digitsStartIdx;
        }
        if (isExponentIndicator(buffer[currentIdx])) {
            floatingPointNumber = true;
            currentIdx++;
            ExponentParsingResult exponentParsingResult = exponentParser.parse(buffer, currentIdx, exponent);
            exponent = exponentParsingResult.exponent();
            currentIdx = exponentParsingResult.currentIdx();
        }
        if (!isStructuralOrWhitespace(buffer[currentIdx])) {
            throw new JsonParsingException("Number has to be followed by a structural character or whitespace.");
        }
        if (floatingPointNumber) {
            double value = doubleParser.parse(buffer, offset, negative, digitsStartIdx, digitCount, digits, exponent);
            tape.appendDouble(value);
        } else {
            if (isOutOfLongRange(negative, digits, digitCount)) {
                throw new JsonParsingException("Number value is out of long range ([" + Long.MIN_VALUE + ", " + Long.MAX_VALUE + "]).");
            }
            tape.appendInt64(negative ? (~digits + 1) : digits);
        }
    }

    byte parseByte(byte[] buffer, int len, int offset) {
        boolean negative = buffer[offset] == '-';

        int currentIdx = negative ? offset + 1 : offset;

        int digitsStartIdx = currentIdx;
        DigitsParsingResult digitsParsingResult = parseDigits(buffer, currentIdx, 0);
        long digits = digitsParsingResult.digits();
        currentIdx = digitsParsingResult.currentIdx();
        int digitCount = currentIdx - digitsStartIdx;
        if (digitCount == 0) {
            throw new JsonParsingException("Invalid number. Minus has to be followed by a digit.");
        }
        if ('0' == buffer[digitsStartIdx] && digitCount > 1) {
            throw new JsonParsingException("Invalid number. Leading zeroes are not allowed.");
        }

        if (currentIdx < len && !isStructuralOrWhitespace(buffer[currentIdx])) {
            throw new JsonParsingException("Number has to be followed by a structural character or whitespace.");
        }
        if (isOutOfByteRange(negative, digits, digitCount)) {
            throw new JsonParsingException("Number value is out of byte range ([" + Byte.MIN_VALUE + ", " + Byte.MAX_VALUE + "]).");
        }
        return (byte) (negative ? (~digits + 1) : digits);
    }

    private static boolean isOutOfByteRange(boolean negative, long digits, int digitCount) {
        if (digitCount < BYTE_MAX_DIGIT_COUNT) {
            return false;
        }
        if (digitCount > BYTE_MAX_DIGIT_COUNT) {
            return true;
        }
        if (negative) {
            return digits > BYTE_MAX_ABS_VALUE;
        }
        return digits > Byte.MAX_VALUE;
    }

    short parseShort(byte[] buffer, int len, int offset) {
        boolean negative = buffer[offset] == '-';

        int currentIdx = negative ? offset + 1 : offset;

        int digitsStartIdx = currentIdx;
        DigitsParsingResult digitsParsingResult = parseDigits(buffer, currentIdx, 0);
        long digits = digitsParsingResult.digits();
        currentIdx = digitsParsingResult.currentIdx();
        int digitCount = currentIdx - digitsStartIdx;
        if (digitCount == 0) {
            throw new JsonParsingException("Invalid number. Minus has to be followed by a digit.");
        }
        if ('0' == buffer[digitsStartIdx] && digitCount > 1) {
            throw new JsonParsingException("Invalid number. Leading zeroes are not allowed.");
        }

        if (currentIdx < len && !isStructuralOrWhitespace(buffer[currentIdx])) {
            throw new JsonParsingException("Number has to be followed by a structural character or whitespace.");
        }
        if (isOutOfShortRange(negative, digits, digitCount)) {
            throw new JsonParsingException("Number value is out of short range ([" + Short.MIN_VALUE + ", " + Short.MAX_VALUE + "]).");
        }
        return (short) (negative ? (~digits + 1) : digits);
    }

    private static boolean isOutOfShortRange(boolean negative, long digits, int digitCount) {
        if (digitCount < SHORT_MAX_DIGIT_COUNT) {
            return false;
        }
        if (digitCount > SHORT_MAX_DIGIT_COUNT) {
            return true;
        }
        if (negative) {
            return digits > SHORT_MAX_ABS_VALUE;
        }
        return digits > Short.MAX_VALUE;
    }

    int parseInt(byte[] buffer, int len, int offset) {
        boolean negative = buffer[offset] == '-';

        int currentIdx = negative ? offset + 1 : offset;

        int digitsStartIdx = currentIdx;
        DigitsParsingResult digitsParsingResult = parseDigits(buffer, currentIdx, 0);
        long digits = digitsParsingResult.digits();
        currentIdx = digitsParsingResult.currentIdx();
        int digitCount = currentIdx - digitsStartIdx;
        if (digitCount == 0) {
            throw new JsonParsingException("Invalid number. Minus has to be followed by a digit.");
        }
        if ('0' == buffer[digitsStartIdx] && digitCount > 1) {
            throw new JsonParsingException("Invalid number. Leading zeroes are not allowed.");
        }

        if (currentIdx < len && !isStructuralOrWhitespace(buffer[currentIdx])) {
            throw new JsonParsingException("Number has to be followed by a structural character or whitespace.");
        }
        if (isOutOfIntRange(negative, digits, digitCount)) {
            throw new JsonParsingException("Number value is out of int range ([" + Integer.MIN_VALUE + ", " + Integer.MAX_VALUE + "]).");
        }
        return (int) (negative ? (~digits + 1) : digits);
    }

    private static boolean isOutOfIntRange(boolean negative, long digits, int digitCount) {
        if (digitCount < INT_MAX_DIGIT_COUNT) {
            return false;
        }
        if (digitCount > INT_MAX_DIGIT_COUNT) {
            return true;
        }
        if (negative) {
            return digits > INT_MAX_ABS_VALUE;
        }
        return digits > Integer.MAX_VALUE;
    }

    long parseLong(byte[] buffer, int len, int offset) {
        boolean negative = buffer[offset] == '-';

        int currentIdx = negative ? offset + 1 : offset;

        int digitsStartIdx = currentIdx;
        DigitsParsingResult digitsParsingResult = parseDigits(buffer, currentIdx, 0);
        long digits = digitsParsingResult.digits();
        currentIdx = digitsParsingResult.currentIdx();
        int digitCount = currentIdx - digitsStartIdx;
        if (digitCount == 0) {
            throw new JsonParsingException("Invalid number. Minus has to be followed by a digit.");
        }
        if ('0' == buffer[digitsStartIdx] && digitCount > 1) {
            throw new JsonParsingException("Invalid number. Leading zeroes are not allowed.");
        }

        if (currentIdx < len && !isStructuralOrWhitespace(buffer[currentIdx])) {
            throw new JsonParsingException("Number has to be followed by a structural character or whitespace.");
        }
        if (isOutOfLongRange(negative, digits, digitCount)) {
            throw new JsonParsingException("Number value is out of long range ([" + Long.MIN_VALUE + ", " + Long.MAX_VALUE + "]).");
        }
        return negative ? (~digits + 1) : digits;
    }

    float parseFloat(byte[] buffer, int len, int offset) {
        boolean negative = buffer[offset] == '-';

        int currentIdx = negative ? offset + 1 : offset;

        int digitsStartIdx = currentIdx;
        DigitsParsingResult digitsParsingResult = parseDigits(buffer, currentIdx, 0);
        currentIdx = digitsParsingResult.currentIdx();
        int digitCount = currentIdx - digitsStartIdx;
        if (digitCount == 0) {
            if (checkIfNull(buffer, len, offset)) return Float.NaN;
            throw new JsonParsingException("Invalid number. Minus has to be followed by a digit.");
        }
        if ('0' == buffer[digitsStartIdx] && digitCount > 1) {
            throw new JsonParsingException("Invalid number. Leading zeroes are not allowed.");
        }

        long exponent = 0;
        boolean floatingPointNumber = false;
        if ('.' == buffer[currentIdx]) {
            floatingPointNumber = true;
            currentIdx++;
            int firstIdxAfterPeriod = currentIdx;
            digitsParsingResult = parseDigits(buffer, currentIdx, digitsParsingResult.digits());
            currentIdx = digitsParsingResult.currentIdx();
            exponent = firstIdxAfterPeriod - currentIdx;
            if (exponent == 0) {
                throw new JsonParsingException("Invalid number. Decimal point has to be followed by a digit.");
            }
            digitCount = currentIdx - digitsStartIdx;
        }
        if (isExponentIndicator(buffer[currentIdx])) {
            floatingPointNumber = true;
            currentIdx++;
            ExponentParsingResult exponentParsingResult = exponentParser.parse(buffer, currentIdx, exponent);
            exponent = exponentParsingResult.exponent();
            currentIdx = exponentParsingResult.currentIdx();
        }
        if (!floatingPointNumber) {
            throw new JsonParsingException("Invalid floating-point number. Fraction or exponent part is missing.");
        }
        if (currentIdx < len && !isStructuralOrWhitespace(buffer[currentIdx])) {
            throw new JsonParsingException("Number has to be followed by a structural character or whitespace.");
        }

        return floatParser.parse(buffer, offset, negative, digitsStartIdx, digitCount, digitsParsingResult.digits(), exponent);
    }

    double parseDouble(byte[] buffer, int len, int offset) {
        boolean negative = buffer[offset] == '-';

        int currentIdx = negative ? offset + 1 : offset;

        int digitsStartIdx = currentIdx;
        DigitsParsingResult digitsParsingResult = parseDigits(buffer, currentIdx, 0);
        currentIdx = digitsParsingResult.currentIdx();
        int digitCount = currentIdx - digitsStartIdx;
        if (digitCount == 0) {
            if (checkIfNull(buffer, len, offset)) return Double.NaN;
            throw new JsonParsingException("Invalid number. Minus has to be followed by a digit.");
        }
        if ('0' == buffer[digitsStartIdx] && digitCount > 1) {
            throw new JsonParsingException("Invalid number. Leading zeroes are not allowed.");
        }

        long exponent = 0;
        boolean floatingPointNumber = false;
        if ('.' == buffer[currentIdx]) {
            floatingPointNumber = true;
            currentIdx++;
            int firstIdxAfterPeriod = currentIdx;
            digitsParsingResult = parseDigits(buffer, currentIdx, digitsParsingResult.digits());
            currentIdx = digitsParsingResult.currentIdx();
            exponent = firstIdxAfterPeriod - currentIdx;
            if (exponent == 0) {
                throw new JsonParsingException("Invalid number. Decimal point has to be followed by a digit.");
            }
            digitCount = currentIdx - digitsStartIdx;
        }
        if (isExponentIndicator(buffer[currentIdx])) {
            floatingPointNumber = true;
            currentIdx++;
            ExponentParsingResult exponentParsingResult = exponentParser.parse(buffer, currentIdx, exponent);
            exponent = exponentParsingResult.exponent();
            currentIdx = exponentParsingResult.currentIdx();
        }
        if (!floatingPointNumber) {
            throw new JsonParsingException("Invalid floating-point number. Fraction or exponent part is missing.");
        }
        if (currentIdx < len && !isStructuralOrWhitespace(buffer[currentIdx])) {
            throw new JsonParsingException("Number has to be followed by a structural character or whitespace.");
        }

        return doubleParser.parse(buffer, offset, negative, digitsStartIdx, digitCount, digitsParsingResult.digits(), exponent);
    }

    private static boolean isOutOfLongRange(boolean negative, long digits, int digitCount) {
        if (digitCount < LONG_MAX_DIGIT_COUNT) {
            return false;
        }
        if (digitCount > LONG_MAX_DIGIT_COUNT) {
            return true;
        }
        if (negative && digits == Long.MIN_VALUE) {
            // The maximum value we can store in a long is 9223372036854775807. When we try to store 9223372036854775808,
            // a long wraps around, resulting in -9223372036854775808 (Long.MIN_VALUE). If the number we are parsing is
            // negative, and we've attempted to store 9223372036854775808 in "digits", we can be sure that we are
            // dealing with Long.MIN_VALUE, which obviously does not fall outside the acceptable range.
            return false;
        }
        return digits < 0;
    }

    private DigitsParsingResult parseDigits(byte[] buffer, int currentIdx, long digits) {
        byte digit = convertCharacterToDigit(buffer[currentIdx]);
        while (digit >= 0 && digit <= 9) {
            digits = 10 * digits + digit;
            currentIdx++;
            digit = convertCharacterToDigit(buffer[currentIdx]);
        }
        return digitsParsingResult.of(digits, currentIdx);
    }

    private static byte convertCharacterToDigit(byte b) {
        return (byte) (b - '0');
    }

    private static class DigitsParsingResult {

        private long digits;
        private int currentIdx;

        DigitsParsingResult of(long digits, int currentIdx) {
            this.digits = digits;
            this.currentIdx = currentIdx;
            return this;
        }

        long digits() {
            return digits;
        }

        int currentIdx() {
            return currentIdx;
        }
    }

    private boolean checkIfNull(byte[] buffer, int len, int offset) {
        boolean statsWithN = buffer[offset] == 'n' || buffer[offset] == 'N';
        boolean endsWithL = buffer[offset + 3] == 'l' || buffer[offset + 3] == 'L';
        return statsWithN && endsWithL;
    }

}
