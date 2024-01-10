package org.pragmatica.lang.type;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Tuple.Tuple2;

import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeTokenTest {

    @Test
    void simpleClassesAreCapturedCorrectly() {
        assertEquals(String.class, new TypeToken<String>() {}.token());
        assertEquals(Integer.class, new TypeToken<Integer>() {}.token());
        assertEquals(Long.class, new TypeToken<Long>() {}.token());
        assertEquals(Double.class, new TypeToken<Double>() {}.token());
        assertEquals(Float.class, new TypeToken<Float>() {}.token());
        assertEquals(Boolean.class, new TypeToken<Boolean>() {}.token());
    }

    @Test
    void nestedClassesAreCapturedCorrectly() {
        var token = (ParameterizedType) (new TypeToken<Option<String>>() {}.token());

        assertEquals(Option.class, token.getRawType());
        assertEquals(String.class, token.getActualTypeArguments()[0]);
    }

    @SuppressWarnings("deprecation")
    @Test
    void nestedClassesAreCapturedCorrectlyAndCanBeAccessedViaClassApi() {
        var token = new TypeToken<Tuple2<Map<String, List<Integer>>, List<Stream<BigDecimal>>>>() {};

        // Top level
        assertEquals(Tuple2.class, token.rawType());
        assertEquals(Tuple2.class, token.typeArgument().unwrap());

        // First type parameter of Tuple2
        assertEquals(Map.class, token.typeArgument(0).unwrap());
        assertEquals(String.class, token.typeArgument(0, 0).unwrap());
        assertEquals(List.class, token.typeArgument(0, 1).unwrap());
        assertEquals(Integer.class, token.typeArgument(0, 1, 0).unwrap());

        // Second type parameter of Tuple2
        assertEquals(List.class, token.typeArgument(1).unwrap());
        assertEquals(Stream.class, token.typeArgument(1, 0).unwrap());
        assertEquals(BigDecimal.class, token.typeArgument(1, 0, 0).unwrap());
    }
}