package org.pragmatica.codec.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Simple implementation of type token which allows to capture full generic type. <br /> In order to use this class, one should create anonymous
 * instance of it with required type:
 * <pre> {@code
 *  new TypeToken<Map<Key, List<Values>>() {}
 * }</pre>
 * <p>
 * Then this instance can be used to retrieve complete generic type of the created instance. Note that this implementation is rudimentary and does not
 * provide any extras, but it's good fit to purposes of capturing parameter type.
 * <p>
 * See <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html">this article</a> for more details.
 */
public abstract class TypeToken<T> implements Comparable<TypeToken<T>> {
    private final Type token;

    protected TypeToken() {
        // Retrieve type eagerly to trigger run-time error closer to the issue location
        if (!(getClass().getGenericSuperclass() instanceof ParameterizedType parameterizedType)) {
            throw new IllegalArgumentException("TypeToken constructed without actual type argument.");
        }
        token = parameterizedType.getActualTypeArguments()[0];
    }

    public Type token() {
        return token;
    }
}
