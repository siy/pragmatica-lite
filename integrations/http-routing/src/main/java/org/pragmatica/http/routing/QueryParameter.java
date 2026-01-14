package org.pragmatica.http.routing;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.pragmatica.http.routing.ParameterError.InvalidParameter;

/**
 * Type-safe query parameter parser.
 * <p>
 * Query parameters are optional by nature - missing parameters return {@code Option.none()}.
 * Invalid values return {@code Result.failure()}.
 *
 * @param <T> the type of the parsed parameter value
 */
@SuppressWarnings("unused")
public interface QueryParameter<T> {
    /**
     * Get the parameter name.
     */
    String name();

    /**
     * Parse query parameter values.
     * Returns {@code Option.none()} if parameter is missing.
     *
     * @param values the list of values for this parameter (null or empty if missing)
     * @return success with optional value, or failure if parsing fails
     */
    Result<Option<T>> parse(List<String> values);

    /**
     * String query parameter - accepts any string value.
     *
     * @param name the parameter name
     */
    static QueryParameter<String> aString(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<String>> parse(List<String> values) {
                return Result.success(firstValue(values));
            }
        };
    }

    /**
     * Integer query parameter - parses signed 32-bit integer.
     *
     * @param name the parameter name
     */
    static QueryParameter<Integer> aInteger(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<Integer>> parse(List<String> values) {
                return firstValue(values)
                                 .fold(() -> Result.success(Option.none()),
                                       value -> Result.lift(_ -> new InvalidParameter("Invalid integer query param '" + name
                                                                                      + "': " + value),
                                                            () -> Option.some(Integer.parseInt(value))));
            }
        };
    }

    /**
     * Long query parameter - parses signed 64-bit integer.
     *
     * @param name the parameter name
     */
    static QueryParameter<Long> aLong(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<Long>> parse(List<String> values) {
                return firstValue(values)
                                 .fold(() -> Result.success(Option.none()),
                                       value -> Result.lift(_ -> new InvalidParameter("Invalid long query param '" + name
                                                                                      + "': " + value),
                                                            () -> Option.some(Long.parseLong(value))));
            }
        };
    }

    /**
     * Boolean query parameter - parses boolean value.
     * Accepts "true"/"false" and "yes"/"no" (case-insensitive).
     *
     * @param name the parameter name
     */
    static QueryParameter<Boolean> aBoolean(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<Boolean>> parse(List<String> values) {
                return firstValue(values)
                                 .fold(() -> Result.success(Option.none()),
                                       value -> {
                                           if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
                                               return Result.success(Option.some(true));
                                           } else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) {
                                               return Result.success(Option.some(false));
                                           }
                                           return new InvalidParameter("Invalid boolean query param '" + name + "': " + value
                                                                       + " (expected true/false or yes/no)").result();
                                       });
            }
        };
    }

    /**
     * Double query parameter - parses 64-bit floating point number.
     *
     * @param name the parameter name
     */
    static QueryParameter<Double> aDouble(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<Double>> parse(List<String> values) {
                return firstValue(values)
                                 .fold(() -> Result.success(Option.none()),
                                       value -> Result.lift(_ -> new InvalidParameter("Invalid double query param '" + name
                                                                                      + "': " + value),
                                                            () -> Option.some(Double.parseDouble(value))));
            }
        };
    }

    /**
     * BigDecimal query parameter - parses arbitrary precision decimal.
     *
     * @param name the parameter name
     */
    static QueryParameter<BigDecimal> aDecimal(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<BigDecimal>> parse(List<String> values) {
                return firstValue(values)
                                 .fold(() -> Result.success(Option.none()),
                                       value -> Result.lift(_ -> new InvalidParameter("Invalid decimal query param '" + name
                                                                                      + "': " + value),
                                                            () -> Option.some(new BigDecimal(value))));
            }
        };
    }

    /**
     * LocalDate query parameter - parses ISO-8601 date.
     * Example: "2023-12-15"
     *
     * @param name the parameter name
     */
    static QueryParameter<LocalDate> aLocalDate(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<LocalDate>> parse(List<String> values) {
                return firstValue(values)
                                 .fold(() -> Result.success(Option.none()),
                                       value -> Result.lift(_ -> new InvalidParameter("Invalid local date query param '" + name
                                                                                      + "': " + value),
                                                            () -> Option.some(LocalDate.parse(value))));
            }
        };
    }

    /**
     * LocalDateTime query parameter - parses ISO-8601 date-time without offset.
     * Example: "2023-12-15T10:30:00"
     *
     * @param name the parameter name
     */
    static QueryParameter<LocalDateTime> aLocalDateTime(String name) {
        return new QueryParameter<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Result<Option<LocalDateTime>> parse(List<String> values) {
                return firstValue(values)
                                 .fold(() -> Result.success(Option.none()),
                                       value -> Result.lift(_ -> new InvalidParameter("Invalid local date-time query param '" + name
                                                                                      + "': " + value),
                                                            () -> Option.some(LocalDateTime.parse(value))));
            }
        };
    }

    /**
     * Extract first value from parameter list, if present.
     */
    private static Option<String> firstValue(List<String> values) {
        return values == null || values.isEmpty()
               ? Option.none()
               : Option.option(values.getFirst());
    }
}
