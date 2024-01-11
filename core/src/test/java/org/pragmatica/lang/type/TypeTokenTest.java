package org.pragmatica.lang.type;

import org.junit.jupiter.api.Assertions;
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

    @Test
    void nestedClassesAreCapturedCorrectlyAndCanBeAccessed() {
        var token = new TypeToken<Tuple2<Map<String, List<Integer>>, List<Stream<BigDecimal>>>>() {};

        // Top level
        assertEquals(Tuple2.class, token.rawType());
        token.typeArgument().onEmpty(Assertions::fail).onPresent(arg -> assertEquals(Tuple2.class, arg));

        // First type parameter of Tuple2
        token.typeArgument(0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(Map.class, arg));
        token.typeArgument(0, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(String.class, arg));
        token.typeArgument(0, 1).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(List.class, arg));
        token.typeArgument(0, 1, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(Integer.class, arg));

        // Second type parameter of Tuple2
        token.typeArgument(1).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(List.class, arg));
        token.typeArgument(1, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(Stream.class, arg));
        token.typeArgument(1, 0, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(BigDecimal.class, arg));
    }

    @Test
    void nestedSubTypesAreCapturedCorrectlyAndCanBeAccessed() {
        var token = new TypeToken<Tuple2<Map<String, List<Integer>>, List<Stream<BigDecimal>>>>() {};

        // Top level
        token.subType().onEmpty(Assertions::fail).onPresent(arg -> assertEquals(token, arg));

        // First type parameter of Tuple2
        token.subType(0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(new TypeToken<Map<String, List<Integer>>>() {}, arg));
        token.subType(0, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(new TypeToken<String>() {}, arg));
        token.subType(0, 1).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(new TypeToken<List<Integer>>() {}, arg));
        token.subType(0, 1, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(new TypeToken<Integer>() {}, arg));

        // Second type parameter of Tuple2
        token.subType(1).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(new TypeToken<List<Stream<BigDecimal>>>() {}, arg));
        token.subType(1, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(new TypeToken<Stream<BigDecimal>>() {}, arg));
        token.subType(1, 0, 0).onEmpty(Assertions::fail).onPresent(arg -> assertEquals(new TypeToken<BigDecimal>() {}, arg));
    }
}