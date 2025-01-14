package com.github.pgasync.conversion;

import com.github.pgasync.Oid;
import com.github.pgasync.net.Converter;
import org.pragmatica.lang.Functions.Fn2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.pgasync.conversion.TemporalConversions.*;
import static com.github.pgasync.util.HexConverter.parseHexBinary;

/**
 * @author Antti Laisi
 */
public class DataConverter {
    private final Map<Class<?>, Converter<?>> typeToConverter;
    private final Charset encoding;

    public DataConverter(List<Converter<?>> converters, Charset encoding) {
        this.typeToConverter = converters.stream()
                                         .collect(Collectors.toMap(Converter::type, Function.identity()));
        this.encoding = encoding;
    }

    public String toString(Oid oid, byte[] value) {
        return value == null ? null : StringConversions.asString(oid, new String(value, encoding));
    }

    public Character toChar(Oid oid, byte[] value) {
        return value == null ? null : StringConversions.toChar(oid, new String(value, encoding));
    }

    public Long toLong(Oid oid, byte[] value) {
        return value == null ? null : NumericConversions.toLong(oid, new String(value, encoding));
    }

    public Integer toInteger(Oid oid, byte[] value) {
        return value == null ? null : NumericConversions.toInteger(oid, new String(value, encoding));
    }

    public Short toShort(Oid oid, byte[] value) {
        return value == null ? null : NumericConversions.toShort(oid, new String(value, encoding));
    }

    public Byte toByte(Oid oid, byte[] value) {
        return value == null ? null : NumericConversions.toByte(oid, new String(value, encoding));
    }

    public BigInteger toBigInteger(Oid oid, byte[] value) {
        return value == null ? null : NumericConversions.toBigInteger(oid, new String(value, encoding));
    }

    public BigDecimal toBigDecimal(Oid oid, byte[] value) {
        return value == null ? null : NumericConversions.toBigDecimal(oid, new String(value, encoding));
    }

    public Double toDouble(Oid oid, byte[] value) {
        return value == null ? null : NumericConversions.toDouble(oid, new String(value, encoding));
    }

    public LocalDate toLocalDate(Oid oid, byte[] value) {
        return value == null ? null : TemporalConversions.toLocalDate(oid, new String(value, encoding));
    }

    public LocalDateTime toLocalDateTime(Oid oid, byte[] value) {
        return value == null ? null : TemporalConversions.toLocalDateTime(oid, new String(value, encoding));
    }

    public LocalTime toLocalTime(Oid oid, byte[] value) {
        return value == null ? null : TemporalConversions.toLocalTime(oid, new String(value, encoding));
    }

    public Instant toInstant(Oid oid, byte[] value) {
        return value == null ? null : TemporalConversions.toInstant(oid, new String(value, encoding));
    }

    public byte[] toBytes(Oid oid, byte[] value) {
        return value == null ? null : BlobConversions.toBytes(oid, new String(value, 2, value.length - 2, encoding));
    }

    public Boolean toBoolean(Oid oid, byte[] value) {
        return value == null ? null : BooleanConversions.toBoolean(oid, new String(value, encoding));
    }

    public <T> T toArray(Class<T> arrayType, Oid oid, byte[] value) {
        if (value == null) {
            return null;
        }

        return ArrayConversions.toArray(arrayType, oid, new String(value, encoding), lookupParser(oid));
    }

    private BiFunction<Oid, String, Object> lookupParser(Oid oid) {
        return switch (oid) {
            case INT2_ARRAY -> NumericConversions::toShort;
            case INT4_ARRAY -> NumericConversions::toInteger;
            case INT8_ARRAY -> NumericConversions::toLong;
            case TEXT_ARRAY, CHAR_ARRAY, BPCHAR_ARRAY, VARCHAR_ARRAY -> StringConversions::asString;
            case NUMERIC_ARRAY, FLOAT4_ARRAY, FLOAT8_ARRAY -> NumericConversions::toBigDecimal;
            case TIMESTAMP_ARRAY, TIMESTAMPTZ_ARRAY -> TemporalConversions::toInstant;
            case TIMETZ_ARRAY, TIME_ARRAY -> TemporalConversions::toLocalTime;
            case DATE_ARRAY -> TemporalConversions::toLocalDate;
            case BOOL_ARRAY -> BooleanConversions::toBoolean;
            case BYTEA_ARRAY -> (oide, svaluee) -> {
                byte[] first = BlobConversions.toBytes(oide, svaluee.substring(2));
                return parseHexBinary(new String(first, 1, first.length - 1, encoding));
            };
            default -> throw new IllegalStateException("Unsupported array type: " + oid);
        };
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<T> getConverter(Class<T> type) {
        var converter = (Converter<T>) typeToConverter.get(type);

        if (converter == null) {
            throw new IllegalArgumentException("Unknown conversion target: " + type);
        }

        return converter;
    }

    private String fromObject(Object o) {
        return switch (o) {
            case null -> null;
            case LocalDate localDate -> fromLocalDate(localDate);
            case LocalTime localTime -> fromLocalTime(localTime);
            case LocalDateTime localDateTime -> fromLocalDateTime(localDateTime);
            case ZonedDateTime zonedDateTime -> fromZoneDateTime(zonedDateTime);
            case OffsetDateTime offsetDateTime -> fromOffsetDateTime(offsetDateTime);
            case Instant instant -> fromInstant(instant);
            case byte[] bytes -> BlobConversions.fromBytes(bytes);
            case Boolean bool -> BooleanConversions.fromBoolean(bool);
            case String _, Number _, Character _, UUID _ -> o.toString();

            default -> {
                if (o.getClass().isArray()) {
                    yield ArrayConversions.fromArray(o, this::fromObject);
                } else {
                    yield fromConvertible(o);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> String fromConvertible(T value) {
        var converter = getConverter((Class<T>) value.getClass());
        return converter.from(value);
    }

    @SuppressWarnings("unused")
    public byte[][] fromParameters(List<Object> parameters) {
        return fromParameters(parameters.toArray(new Object[]{}));
    }

    public byte[][] fromParameters(Object[] parameters) {
        byte[][] params = new byte[parameters.length][];
        int i = 0;
        for (var param : parameters) {
            var converted = fromObject(param);
            params[i++] = converted == null ? null : converted.getBytes(encoding);
        }
        return params;
    }

    private static final Map<Class<?>, Fn2<?, Oid, String>> KNOWN_TYPES = new HashMap<>();

    static {
        KNOWN_TYPES.put(byte.class, NumericConversions::toByte);
        KNOWN_TYPES.put(Byte.class, NumericConversions::toByte);
        KNOWN_TYPES.put(char.class, StringConversions::toChar);
        KNOWN_TYPES.put(Character.class, StringConversions::toChar);
        KNOWN_TYPES.put(short.class, NumericConversions::toShort);
        KNOWN_TYPES.put(Short.class, NumericConversions::toShort);
        KNOWN_TYPES.put(int.class, NumericConversions::toInteger);
        KNOWN_TYPES.put(Integer.class, NumericConversions::toInteger);
        KNOWN_TYPES.put(long.class, NumericConversions::toLong);
        KNOWN_TYPES.put(Long.class, NumericConversions::toLong);
        KNOWN_TYPES.put(BigInteger.class, NumericConversions::toBigInteger);
        KNOWN_TYPES.put(BigDecimal.class, NumericConversions::toBigDecimal);
        KNOWN_TYPES.put(float.class, NumericConversions::toFloat);
        KNOWN_TYPES.put(Float.class, NumericConversions::toFloat);
        KNOWN_TYPES.put(double.class, NumericConversions::toDouble);
        KNOWN_TYPES.put(Double.class, NumericConversions::toDouble);
        KNOWN_TYPES.put(String.class, StringConversions::asString);
        KNOWN_TYPES.put(LocalDate.class, TemporalConversions::toLocalDate);
        KNOWN_TYPES.put(LocalTime.class, TemporalConversions::toLocalTime);
        KNOWN_TYPES.put(LocalDateTime.class, TemporalConversions::toLocalDateTime);
        KNOWN_TYPES.put(ZonedDateTime.class, TemporalConversions::toZonedDateTime);
        KNOWN_TYPES.put(OffsetDateTime.class, TemporalConversions::toOffsetDateTime);
        KNOWN_TYPES.put(Instant.class, TemporalConversions::toInstant);
    }

    @SuppressWarnings("unchecked")
    public <T> T toObject(Oid oid, byte[] value, Class<T> type) {
        if (value == null) {
            return null;
        }

        if (type != null) {
            // Try custom converter first
            var converter = (Converter<T>) typeToConverter.get(type);

            if (converter != null) {
                return converter.to(oid, new String(value, encoding));
            }

            // Try known converter
            var knownConverter = KNOWN_TYPES.get(type);

            if (knownConverter != null) {
                return (T) knownConverter.apply(oid, new String(value, encoding));
            }

            throw new IllegalArgumentException("Unknown conversion target: " + type);
        }

        // Convert by oid
        return (T) switch (oid) {
            case null -> null;
            case TEXT, CHAR, BPCHAR, VARCHAR -> toString(oid, value);
            case INT2 -> toShort(oid, value);
            case INT4 -> toInteger(oid, value);
            case INT8 -> toLong(oid, value);
            case NUMERIC, FLOAT4, FLOAT8 -> toBigDecimal(oid, value);
            case BYTEA -> toBytes(oid, value);
            case DATE -> toLocalDate(oid, value);
            case TIMETZ, TIME -> toLocalTime(oid, value);
            case TIMESTAMP, TIMESTAMPTZ -> toInstant(oid, value);
            case UUID -> UUID.fromString(toString(oid, value));
            case BOOL -> toBoolean(oid, value);
            case INT2_ARRAY, INT4_ARRAY, INT8_ARRAY, NUMERIC_ARRAY, FLOAT4_ARRAY, FLOAT8_ARRAY,
                TEXT_ARRAY, CHAR_ARRAY, BPCHAR_ARRAY, VARCHAR_ARRAY,
                TIMESTAMP_ARRAY, TIMESTAMPTZ_ARRAY, TIMETZ_ARRAY, TIME_ARRAY, BOOL_ARRAY -> toArray(Object[].class, oid, value);
            default -> throw new IllegalArgumentException("Unknown conversion target: " + oid);
        };
    }

    public Oid[] assumeTypes(Object... params) {
        var types = new Oid[params.length];

        for (int i = 0; i < params.length; i++) {
            switch (params[i]) {
                case byte[] _ -> types[i] = Oid.BYTEA;
                case Short _, Byte _ -> types[i] = Oid.INT2;
                case Integer _ -> types[i] = Oid.INT4;
                case Long _ -> types[i] = Oid.INT8;
                case Float _ -> types[i] = Oid.FLOAT4;
                case Double _ -> types[i] = Oid.FLOAT8;
                case double[] _ -> types[i] = Oid.FLOAT8_ARRAY;
                case float[] _ -> types[i] = Oid.FLOAT4_ARRAY;
                case long[] _ -> types[i] = Oid.INT8_ARRAY;
                case int[] _ -> types[i] = Oid.INT4_ARRAY;
                case short[] _ -> types[i] = Oid.INT2_ARRAY;
                case byte[][] _ -> types[i] = Oid.BYTEA_ARRAY;
                case BigInteger _, BigDecimal _ -> types[i] = Oid.NUMERIC;
                case BigInteger[] _, BigDecimal[] _ -> types[i] = Oid.NUMERIC_ARRAY;
                case Boolean _ -> types[i] = Oid.BOOL;
                case Boolean[] _ -> types[i] = Oid.BOOL_ARRAY;
                case CharSequence _, Character _ -> types[i] = Oid.VARCHAR;
                case LocalDateTime _, Instant _ -> types[i] = Oid.TIMESTAMP;
                case ZonedDateTime _, OffsetDateTime _ -> types[i] = Oid.TIMESTAMPTZ;
                case LocalDate _ -> types[i] = Oid.DATE;
                case OffsetTime _ -> types[i] = Oid.TIME;
                case LocalTime _ -> types[i] = Oid.TIMETZ;
                case UUID _ -> types[i] = Oid.UUID;
                case null, default -> types[i] = Oid.UNSPECIFIED;
            }
        }
        return types;
    }
}
