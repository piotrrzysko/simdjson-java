package org.simdjson.demand;

import static org.simdjson.testutils.SimdJsonAssertions.assertThat;
import static org.simdjson.testutils.TestUtils.toUtf8;

import org.junit.jupiter.api.Test;
import org.simdjson.PathsBasedJsonParser;

public class PathsBasedTest {
    @Test
    public void testParseObjectWithDefaultTypeString() {
        byte[] bytes = toUtf8("{\"first\": 1, \"field\": 2, \"second\": 3}");
        PathsBasedJsonParser parser = new PathsBasedJsonParser("first", "second", "third");
        Object[] result = parser.parse(bytes, bytes.length);
        assertThat(result[0]).isEqualTo("1");
        assertThat(result[1]).isEqualTo("3");
        assertThat(result[2]).isEqualTo(null);
    }

    @Test
    public void testParseObjectWithType() {
        byte[] bytes = toUtf8("{\"first\": 1, \"field\": 2, \"second\": 3}");
        PathsBasedJsonParser parser = new PathsBasedJsonParser("first:INT", "second:INT", "third:INT");
        Object[] result = parser.parse(bytes, bytes.length);
        assertThat(result[0]).isEqualTo(1);
        assertThat(result[1]).isEqualTo(3);
        assertThat(result[2]).isEqualTo(null);
    }

    @Test
    public void testParseArrayWithDefaultTypeString() {
        byte[] bytes = toUtf8("[1, \"a\", {\"[first]\": 1, \"field\": 2, \"second\": 3}]");
        PathsBasedJsonParser parser = new PathsBasedJsonParser("[0]", "[2].[[first]]", "[2].second", "[2]");
        Object[] result = parser.parse(bytes, bytes.length);
        assertThat(result[0]).isEqualTo("1");
        assertThat(result[1]).isEqualTo("1");
        assertThat(result[2]).isEqualTo("3");
        assertThat(result[3]).isEqualTo("{\"[first]\": 1, \"field\": 2, \"second\": 3}");
    }

    @Test
    public void testParseArrayWithType() {
        byte[] bytes = toUtf8("[1, \"a\", {\"[first]\": 1, \"field\": 2, \"second\": 3}]");
        PathsBasedJsonParser parser = new PathsBasedJsonParser("[0]:INT", "[2].[[first]]:INT", "[2].second:INT", "[2]:STRING");
        Object[] result = parser.parse(bytes, bytes.length);
        assertThat(result[0]).isEqualTo(1);
        assertThat(result[1]).isEqualTo(1);
        assertThat(result[2]).isEqualTo(3);
        assertThat(result[3]).isEqualTo("{\"[first]\": 1, \"field\": 2, \"second\": 3}");
    }

    @Test
    public void testFieldNamesWithNonAsciiCharacters() {
        byte[] json = toUtf8("{\"ąćśńźż\": 1, \"\\u20A9\\u0E3F\": 2, \"αβγ\": 3, \"😀abc😀\": 4}");
        PathsBasedJsonParser parser = new PathsBasedJsonParser("ąćśńźż:INT", "\\u20A9\\u0E3F:INT", "αβγ:INT", "😀abc😀:INT");
        Object[] result = parser.parse(json, json.length);
        assertThat(result[0]).isEqualTo(1);
        assertThat(result[1]).isEqualTo(2);
        assertThat(result[2]).isEqualTo(3);
        assertThat(result[3]).isEqualTo(4);
    }

    @Test
    public void testComplexJson() {
        byte[] json =
                toUtf8("{\"object1\":{\"field1\":123,\"field2\":\"xyz\",\"field3\":3.14,\"field4\":true,\"field5\":null,\"field6\":{\"field7\":{\"field8\":\"abc\"},\"field9\":[1,2,3,4],\"field10\":[1,\"xyz\",1.1,[1,2,3]]}},\"object2\":[\"xyz\",{},123,null,[[1,2,3],[4,5,6]]]}");
        PathsBasedJsonParser parser = new PathsBasedJsonParser("object1.field1:INT", "object1.field3:DOUBLE", "object1.field6.field9",
                "object1.field6.field9.[0]:INT", "object1.field6.field9.[0].[0]", "object2.[4]", "object2.[4].[1]", "object2.[4].[1].[1]:INT");
        Object[] result = parser.parse(json, json.length);
        assertThat(result[0]).isEqualTo(123);
        assertThat(result[1]).isEqualTo(3.14);
        assertThat(result[2]).isEqualTo("[1,2,3,4]");
        assertThat(result[3]).isEqualTo(1);
        assertThat(result[4]).isEqualTo(null);
        assertThat(result[5]).isEqualTo("[[1,2,3],[4,5,6]]");
        assertThat(result[6]).isEqualTo("[4,5,6]");
        assertThat(result[7]).isEqualTo(5);
    }

}
