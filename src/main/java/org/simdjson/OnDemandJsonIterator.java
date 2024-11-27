package org.simdjson;

import java.util.Arrays;

import static org.simdjson.CharacterUtils.isStructuralOrWhitespace;

class OnDemandJsonIterator {

    private static final byte SPACE = 0x20;
    private static final int[] SKIP_DEPTH_PER_CHARACTER = new int[127];

    static {
        Arrays.fill(SKIP_DEPTH_PER_CHARACTER, 0);
        SKIP_DEPTH_PER_CHARACTER['['] = 1;
        SKIP_DEPTH_PER_CHARACTER['{'] = 1;
        SKIP_DEPTH_PER_CHARACTER[']'] = -1;
        SKIP_DEPTH_PER_CHARACTER['}'] = -1;
    }

    private final BitIndexes indexer;
    private final int padding;
    private final StringParser stringParser = new StringParser();
    private final NumberParser numberParser = new NumberParser();

    private byte[] buffer;
    private int len;
    private int depth;

    OnDemandJsonIterator(BitIndexes indexer, int padding) {
        this.indexer = indexer;
        this.padding = padding;
    }

    void init(byte[] buffer, int len) {
        if (indexer.isEnd()) {
            throw new JsonParsingException("No structural element found.");
        }
        this.buffer = buffer;
        this.len = len;
        this.depth = 1;
    }
    void skipChild() {
        skipChild(depth - 1);
    }

    void skipChild(int parentDepth) {
        if (depth <= parentDepth) {
            return;
        }
        int idx = indexer.getAndAdvance();
        byte character = buffer[idx];

        switch (character) {
            case '[', '{', ':', ',':
                break;
            case '"':
                if (buffer[indexer.peek()] == ':') {
                    indexer.advance(); // skip ':'
                    break;
                }
            default:
                depth--;
                if (depth <= parentDepth) {
                    return;
                }
        }

        while (indexer.hasNext()) {
            idx = indexer.getAndAdvance();
            character = buffer[idx];

            int delta = SKIP_DEPTH_PER_CHARACTER[character];
            depth += delta;
            if (delta < 0 && depth <= parentDepth) {
                return;
            }
        }

        throw new JsonParsingException("Not enough close braces.");
    }

    Boolean getRootNonNullBoolean() {
        int idx = indexer.getAndAdvance();
        Boolean result = switch (buffer[idx]) {
            case 't' -> visitRootTrueAtom(idx);
            case 'f' -> visitRootFalseAtom(idx);
            default -> throw new JsonParsingException("Unrecognized boolean value. Expected: 'true' or 'false'.");
        };
        assertNoMoreJsonValues();
        depth--;
        return result;
    }

    Boolean getRootBoolean() {
        int idx = indexer.getAndAdvance();
        Boolean result = switch (buffer[idx]) {
            case 't' -> visitRootTrueAtom(idx);
            case 'f' -> visitRootFalseAtom(idx);
            case 'n' -> {
                visitRootNullAtom(idx);
                yield null;
            }
            default -> throw new JsonParsingException("Unrecognized boolean value. Expected: 'true', 'false' or 'null'.");
        };
        assertNoMoreJsonValues();
        depth--;
        return result;
    }

    private Boolean visitRootTrueAtom(int idx) {
        boolean valid = idx + 4 <= len && isTrue(idx) && (idx + 4 == len || isStructuralOrWhitespace(buffer[idx + 4]));
        if (!valid) {
            throw new JsonParsingException("Invalid value starting at " + idx + ". Expected 'true'.");
        }
        return Boolean.TRUE;
    }

    private Boolean visitRootFalseAtom(int idx) {
        boolean valid = idx + 5 <= len && isFalse(idx) && (idx + 5 == len || isStructuralOrWhitespace(buffer[idx + 5]));
        if (!valid) {
            throw new JsonParsingException("Invalid value starting at " + idx + ". Expected 'false'.");
        }
        return Boolean.FALSE;
    }

    private void visitRootNullAtom(int idx) {
        boolean valid = idx + 4 <= len && isNull(idx) && (idx + 4 == len || isStructuralOrWhitespace(buffer[idx + 4]));
        if (!valid) {
            throw new JsonParsingException("Invalid value starting at " + idx + ". Expected 'null'.");
        }
    }

    private void visitNullAtom(int idx) {
        if (!isNull(idx)) {
            throw new JsonParsingException("Invalid value starting at " + idx + ". Expected 'null'.");
        }
    }

    private boolean isNull(int idx) {
        return buffer[idx] == 'n'
                && buffer[idx + 1] == 'u'
                && buffer[idx + 2] == 'l'
                && buffer[idx + 3] == 'l';
    }

    Boolean getNonNullBoolean() {
        int idx = indexer.getAndAdvance();
        Boolean result = switch (buffer[idx]) {
            case 't' -> visitTrueAtom(idx);
            case 'f' -> visitFalseAtom(idx);
            default -> throw new JsonParsingException("Unrecognized boolean value. Expected: 'true' or 'false'.");
        };
        depth--;
        return result;
    }

    Boolean getBoolean() {
        int idx = indexer.getAndAdvance();
        Boolean result = switch (buffer[idx]) {
            case 't' -> visitTrueAtom(idx);
            case 'f' -> visitFalseAtom(idx);
            case 'n' -> {
                visitNullAtom(idx);
                yield null;
            }
            default -> throw new JsonParsingException("Unrecognized boolean value. Expected: 'true', 'false' or 'null'.");
        };
        depth--;
        return result;
    }

    private Boolean visitTrueAtom(int idx) {
        boolean valid = isTrue(idx) && isStructuralOrWhitespace(buffer[idx + 4]);
        if (!valid) {
            throw new JsonParsingException("Invalid value starting at " + idx + ". Expected 'true'.");
        }
        return Boolean.TRUE;
    }

    private boolean isTrue(int idx) {
        return buffer[idx] == 't'
                && buffer[idx + 1] == 'r'
                && buffer[idx + 2] == 'u'
                && buffer[idx + 3] == 'e';
    }

    private Boolean visitFalseAtom(int idx) {
        boolean valid = isFalse(idx) && isStructuralOrWhitespace(buffer[idx + 5]);
        if (!valid) {
            throw new JsonParsingException("Invalid value starting at " + idx + ". Expected 'false'.");
        }
        return Boolean.FALSE;
    }

    private boolean isFalse(int idx) {
        return buffer[idx] == 'f'
                && buffer[idx + 1] == 'a'
                && buffer[idx + 2] == 'l'
                && buffer[idx + 3] == 's'
                && buffer[idx + 4] == 'e';
    }

    byte getRootNonNullByte() {
        depth--;
        int idx = indexer.getAndAdvance();
        byte[] copy = padRootNumber(idx);
        byte value = numberParser.parseByte(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    Byte getRootByte() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            assertNoMoreJsonValues();
            return null;
        }
        byte[] copy = padRootNumber(idx);
        byte value = numberParser.parseByte(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    byte getNonNullByte() {
        depth--;
        int idx = indexer.getAndAdvance();
        return numberParser.parseByte(buffer, len, idx);
    }

    Byte getByte() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            return null;
        }
        return numberParser.parseByte(buffer, len, idx);
    }

    short getRootNonNullShort() {
        depth--;
        int idx = indexer.getAndAdvance();
        byte[] copy = padRootNumber(idx);
        short value = numberParser.parseShort(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    Short getRootShort() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            assertNoMoreJsonValues();
            return null;
        }
        byte[] copy = padRootNumber(idx);
        short value = numberParser.parseShort(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    short getNonNullShort() {
        depth--;
        int idx = indexer.getAndAdvance();
        return numberParser.parseShort(buffer, len, idx);
    }

    Short getShort() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            return null;
        }
        return numberParser.parseShort(buffer, len, idx);
    }

    int getRootNonNullInt() {
        depth--;
        int idx = indexer.getAndAdvance();
        byte[] copy = padRootNumber(idx);
        int value = numberParser.parseInt(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    Integer getRootInt() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            assertNoMoreJsonValues();
            return null;
        }
        byte[] copy = padRootNumber(idx);
        int value = numberParser.parseInt(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    Integer getInt() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            return null;
        }
        return numberParser.parseInt(buffer, len, idx);
    }

    int getNonNullInt() {
        depth--;
        int idx = indexer.getAndAdvance();
        return numberParser.parseInt(buffer, len, idx);
    }

    long getRootNonNullLong() {
        depth--;
        int idx = indexer.getAndAdvance();
        byte[] copy = padRootNumber(idx);
        long value = numberParser.parseLong(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    Long getRootLong() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            assertNoMoreJsonValues();
            return null;
        }
        byte[] copy = padRootNumber(idx);
        long value = numberParser.parseLong(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    long getNonNullLong() {
        depth--;
        int idx = indexer.getAndAdvance();
        return numberParser.parseLong(buffer, len, idx);
    }

    Long getLong() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            return null;
        }
        return numberParser.parseLong(buffer, len, idx);
    }

    float getRootNonNullFloat() {
        depth--;
        int idx = indexer.getAndAdvance();
        byte[] copy = padRootNumber(idx);
        float value = numberParser.parseFloat(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    Float getRootFloat() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            assertNoMoreJsonValues();
            return null;
        }
        byte[] copy = padRootNumber(idx);
        float value = numberParser.parseFloat(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    double getRootNonNullDouble() {
        depth--;
        int idx = indexer.getAndAdvance();
        byte[] copy = padRootNumber(idx);
        double value = numberParser.parseDouble(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    Double getRootDouble() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            assertNoMoreJsonValues();
            return null;
        }
        byte[] copy = padRootNumber(idx);
        double value = numberParser.parseDouble(copy, len, 0);
        assertNoMoreJsonValues();
        return value;
    }

    private byte[] padRootNumber(int idx) {
        int remainingLen = len - idx;
        byte[] copy = new byte[remainingLen + padding];
        System.arraycopy(buffer, idx, copy, 0, remainingLen);
        Arrays.fill(copy, remainingLen, remainingLen + padding, SPACE);
        return copy;
    }

    double getNonNullDouble() {
        depth--;
        int idx = indexer.getAndAdvance();
        return numberParser.parseDouble(buffer, len, idx);
    }

    Double getDouble() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            return null;
        }
        return numberParser.parseDouble(buffer, len, idx);
    }

    float getNonNullFloat() {
        depth--;
        int idx = indexer.getAndAdvance();
        return numberParser.parseFloat(buffer, len, idx);
    }

    Float getFloat() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            return null;
        }
        return numberParser.parseFloat(buffer, len, idx);
    }
    String getOrCompressAsString() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == '"') {
            return new String(buffer, idx + 1, indexer.peek() - idx - 2);
        } else {
            return new String(buffer, idx, indexer.peek() - idx);
        }
    }
    String getObjectKey() {
        int idx = indexer.getAndAdvance();
        return new String(buffer, idx + 1, indexer.peek() - idx - 2);
    }

    int getRootString(byte[] stringBuffer) {
        depth--;
        int idx = indexer.getAndAdvance();
        int len = switch (buffer[idx]) {
            case '"' -> stringParser.parseString(buffer, idx, stringBuffer);
            case 'n' -> {
                visitRootNullAtom(idx);
                yield -1;
            }
            default -> throw new JsonParsingException("Invalid value starting at " + idx + ". Expected either string or 'null'.");
        };
        assertNoMoreJsonValues();
        return len;
    }

    int getString(byte[] stringBuffer) {
        depth--;
        int idx = indexer.getAndAdvance();
        return switch (buffer[idx]) {
            case '"' -> stringParser.parseString(buffer, idx, stringBuffer);
            case 'n' -> {
                visitNullAtom(idx);
                yield -1;
            }
            default -> throw new JsonParsingException("Invalid value starting at " + idx + ". Expected either string or 'null'.");
        };
    }

    char getNonNullChar() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == '"') {
            return stringParser.parseChar(buffer, idx);
        }
        throw new JsonParsingException("Invalid value starting at " + idx + ". Expected string.");
    }

    Character getChar() {
        depth--;
        int idx = indexer.getAndAdvance();
        return switch (buffer[idx]) {
            case '"' -> stringParser.parseChar(buffer, idx);
            case 'n' -> {
                visitNullAtom(idx);
                yield null;
            }
            default -> throw new JsonParsingException("Invalid value starting at " + idx + ". Expected either string or 'null'.");
        };
    }

    char getRootNonNullChar() {
        depth--;
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == '"') {
            char character = stringParser.parseChar(buffer, idx);
            assertNoMoreJsonValues();
            return character;
        }
        throw new JsonParsingException("Invalid value starting at " + idx + ". Expected string.");
    }

    Character getRootChar() {
        depth--;
        int idx = indexer.getAndAdvance();
        Character character = switch (buffer[idx]) {
            case '"' -> stringParser.parseChar(buffer, idx);
            case 'n' -> {
                visitRootNullAtom(idx);
                yield null;
            }
            default -> throw new JsonParsingException("Invalid value starting at " + idx + ". Expected either string or 'null'.");
        };
        assertNoMoreJsonValues();
        return character;
    }

    IteratorResult startIteratingArray() {
        int idx = indexer.peek();
        if (buffer[idx] == 'n') {
            visitNullAtom(idx);
            indexer.advance();
            depth--;
            return IteratorResult.NULL;
        }
        if (buffer[idx] != '[') {
            throw unexpectedCharException(idx, '[');
        }
        idx = indexer.advanceAndGet();
        if (buffer[idx] == ']') {
            indexer.advance();
            depth--;
            return IteratorResult.EMPTY;
        }
        depth++;
        return IteratorResult.NOT_EMPTY;
    }

    IteratorResult startIteratingRootArray() {
        int idx = indexer.peek();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            indexer.advance();
            depth--;
            return IteratorResult.NULL;
        }
        if (buffer[idx] != '[') {
            throw unexpectedCharException(idx, '[');
        }
        if (buffer[indexer.getLast()] != ']') {
            throw new JsonParsingException("Unclosed array. Missing ']' for starting '['.");
        }
        idx = indexer.advanceAndGet();
        if (buffer[idx] == ']') {
            indexer.advance();
            depth--;
            assertNoMoreJsonValues();
            return IteratorResult.EMPTY;
        }
        depth++;
        return IteratorResult.NOT_EMPTY;
    }

    boolean nextArrayElement() {
        int idx = indexer.getAndAdvance();
        if (buffer[idx] == ']') {
            depth--;
            return false;
        } else if (buffer[idx] == ',') {
            depth++;
            return true;
        } else {
            throw new JsonParsingException("Missing comma between array values");
        }
    }

    IteratorResult startIteratingObject() {
        int idx = indexer.peek();
        if (buffer[idx] == 'n') {
            visitNullAtom(idx);
            indexer.advance();
            depth--;
            return IteratorResult.NULL;
        }
        if (buffer[idx] != '{') {
            throw unexpectedCharException(idx, '{');
        }
        idx = indexer.advanceAndGet();
        if (buffer[idx] == '}') {
            indexer.advance();
            depth--;
            return IteratorResult.EMPTY;
        }
        return IteratorResult.NOT_EMPTY;
    }

    IteratorResult startIteratingRootObject() {
        int idx = indexer.peek();
        if (buffer[idx] == 'n') {
            visitRootNullAtom(idx);
            indexer.advance();
            depth--;
            return IteratorResult.NULL;
        }
        if (buffer[idx] != '{') {
            throw unexpectedCharException(idx, '{');
        }
        if (buffer[indexer.getLast()] != '}') {
            throw new JsonParsingException("Unclosed object. Missing '}' for starting '{'.");
        }
        idx = indexer.advanceAndGet();
        if (buffer[idx] == '}') {
            indexer.advance();
            depth--;
            assertNoMoreJsonValues();
            return IteratorResult.EMPTY;
        }
        return IteratorResult.NOT_EMPTY;
    }
    boolean nextObjectField() {
        int idx = indexer.getAndAdvance();
        byte character = buffer[idx];
        if (character == '}') {
            depth--;
            return false;
        } else if (character == ',') {
            return true;
        } else {
            throw unexpectedCharException(idx, ',');
        }
    }

    void moveToFieldValue() {
        int idx = indexer.getAndAdvance();
        if (buffer[idx] != ':') {
            throw unexpectedCharException(idx, ':');
        }
        depth++;
    }

    int getFieldName(byte[] stringBuffer) {
        int idx = indexer.getAndAdvance();
        if (buffer[idx] != '"') {
            throw unexpectedCharException(idx, '"');
        }
        return stringParser.parseString(buffer, idx, stringBuffer);
    }

    int getDepth() {
        return depth;
    }

    private JsonParsingException unexpectedCharException(int idx, char expected) {
        if (indexer.isPastEnd()) {
            return new JsonParsingException("Expected '" + expected + "' but reached end of buffer.");
        } else {
            return new JsonParsingException("Expected '" + expected + "' but got: '" + (char) buffer[idx] + "'.");
        }
    }

    void assertNoMoreJsonValues() {
        if (indexer.hasNext()) {
            throw new JsonParsingException("More than one JSON value at the root of the document, or extra characters at the end of the JSON!");
        }
    }

    enum IteratorResult {
        EMPTY, NULL, NOT_EMPTY
    }
}
