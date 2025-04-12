package org.pragmatica.lang.type;

import org.pragmatica.lang.Option;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/// Simple implementation of type token which allows to capture full generic type.
/// In order to use this class, one should create anonymous
/// instance of it with required type:
/// <pre>
/// `new TypeToken<Map<Key, List<Values>>(){}`</pre>
///
/// Then this instance can be used to retrieve complete generic type of the created instance. Note that this implementation is rudimentary and does not
/// provide any extras, but it's good fit to purposes of capturing parameter type.
///
/// See <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html">this article</a> for more details.
public abstract class TypeToken<T> implements Comparable<TypeToken<T>> {
    private final Type token;

    protected TypeToken(Type token) {
        this.token = token;
    }

    protected TypeToken() {
        // Retrieve type eagerly to trigger run-time error closer to the issue location
        if (!(getClass().getGenericSuperclass() instanceof ParameterizedType parameterizedType)) {
            throw new IllegalArgumentException("TypeToken constructed without actual type argument.");
        }

        token = parameterizedType.getActualTypeArguments()[0];
    }

    public static <T> TypeToken<T> of(Class<T> clazz) {
        return new TypeToken<>(clazz) {};
    }

    public Type token() {
        return token;
    }

    public Class<?> rawType() {
        return rawClass(token);
    }

    /// Return type arguments starting from the most outer one. Each index points to elements at given level of nesting.
    /// For example, for `Map<Key, List<Value>>`:
    /// `typeArgument()` returns `Map.class`
    /// `typeArgument(0)` returns `Key.class`
    /// `typeArgument(1)` returns `List.class`
    /// `typeArgument(1, 0)` returns `Value.class`
    /// I.e. First argument points to the type arguments of the outer type. Second - to the type arguments of the type
    /// argument of outer type selected by first argument. And so on.
    ///
    /// @param indexes Indexes of type arguments
    ///
    /// @return type argument at the specified chain of indexes or empty option some index points to the non-existent type argument
    public Option<Class<?>> typeArgument(int ... indexes) {
        if (indexes.length == 0) {
            return Option.option(rawClass(token));
        }

        for (var ndx : indexes) {
            if (ndx < 0) {
                return Option.none();
            }
        }

        return recursivelyGetType(token, indexes);
    }

    private static Option<Class<?>> recursivelyGetType(Type type, int... indexes) {
        var index = indexes[0];

        if (!(type instanceof ParameterizedType parameterizedType)) {
            return Option.none();
        }

        if (parameterizedType.getActualTypeArguments().length <= index) {
            return Option.none();
        }

        var actualTypeArgument = parameterizedType.getActualTypeArguments()[index];

        if (indexes.length == 1) {
            return Option.option(rawClass(actualTypeArgument));
        } else {
            return recursivelyGetType(actualTypeArgument, Arrays.copyOfRange(indexes, 1, indexes.length));
        }
    }

    private static Class<?> rawClass(Type type) {
        return switch (type) {
            case Class<?> clazz -> clazz;
            case ParameterizedType parameterizedType -> (Class<?>) parameterizedType.getRawType();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public Option<TypeToken<?>> subType(int ... indexes) {
        if (indexes.length == 0) {
            return Option.option(this);
        }

        for (var ndx : indexes) {
            if (ndx < 0) {
                return Option.none();
            }
        }

        return recursivelyGetSubType(token, indexes);
    }

    private static Option<TypeToken<?>> recursivelyGetSubType(Type type, int... indexes) {
        var index = indexes[0];

        if (!(type instanceof ParameterizedType parameterizedType)) {
            return Option.none();
        }

        if (parameterizedType.getActualTypeArguments().length <= index) {
            return Option.none();
        }

        var actualTypeArgument = parameterizedType.getActualTypeArguments()[index];

        if (indexes.length == 1) {
            return Option.option(new TypeToken<>(actualTypeArgument) {});
        } else {
            return recursivelyGetSubType(actualTypeArgument, Arrays.copyOfRange(indexes, 1, indexes.length));
        }
    }

    public int compareTo(TypeToken<T> o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof TypeToken<?> typeToken) {
            return token.equals(typeToken.token);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public String toString() {
        return "TypeToken<" + token + '>';
    }
}
