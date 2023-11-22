package org.pragmatica.http.util;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Tuple.Tuple2;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static org.pragmatica.lang.Option.some;
import static org.pragmatica.lang.Tuple.tuple;

/**
 * Utility class for decoding query strings. Somewhat modified version of code taken from <a href="https://stackoverflow.com/a/58017826">here</a>.
 */
public class QueryStringDecoder {
    /**
     * Decode parameters in query part of a URI into a map from parameter name to its parameter values. For parameters that occur multiple times each
     * value is collected. Proper decoding of the parameters is performed.
     * <p>
     * Example
     * <pre>a=1&b=2&c=&a=4</pre>
     * is converted into
     * <pre>{a=[Option(1), Option(4)], b=[Option(2)], c=[Option.empty]}</pre>
     *
     * @param query the query part of a URI
     *
     * @return map of parameters names into a list of their values.
     */
    public static Map<String, List<Option<String>>> splitQuery(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        return Stream.of(query.split("&"))
                     .map(QueryStringDecoder::splitQueryParameter)
                     .collect(groupingBy(Tuple2::first, // group by parameter name
                                         mapping(Tuple2::last, toList())));// keep parameter values and assemble into list
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static Tuple2<String, Option<String>> splitQueryParameter(String parameter) {
        var keyValue = parameter.split("=");

        var key = decode(keyValue[0]);
        var value = keyValue.length > 1
                    ? some(decode(keyValue[1]))
                    : Option.<String>empty();

        return tuple(key, value);
    }
}