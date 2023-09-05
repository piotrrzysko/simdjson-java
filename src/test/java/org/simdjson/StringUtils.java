package org.simdjson;

import jdk.incubator.vector.ByteVector;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class StringUtils {

    static String padWithSpaces(String str) {
        int targetLength = 64;
        byte[] strBytes = str.getBytes(UTF_8);
        assertThat(strBytes.length).isLessThanOrEqualTo(targetLength);
        byte[] padded = new byte[targetLength];
        Arrays.fill(padded, (byte) ' ');
        System.arraycopy(strBytes, 0, padded, 0, strBytes.length);
        return new String(padded, UTF_8);
    }

    static ByteVector chunk(String str, int n) {
        return ByteVector.fromArray(StructuralIndexer.SPECIES, str.getBytes(UTF_8), n * StructuralIndexer.SPECIES.vectorByteSize());
    }
}
