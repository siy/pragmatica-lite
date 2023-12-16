package com.github.pgasync.conversion;

import com.github.pgasync.Oid;
import com.github.pgasync.net.Converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
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
        return value == null ? null : StringConversions.toString(oid, new String(value, encoding));
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

    //TODO: get rid of it
    public Date toDate(Oid oid, byte[] value) {
        return value == null ? null : TemporalConversions.toDate(oid, new String(value, encoding));
    }

    public Time toTime(Oid oid, byte[] value) {
        return value == null ? null : TemporalConversions.toTime(oid, new String(value, encoding));
    }

    public Timestamp toTimestamp(Oid oid, byte[] value) {
        return value == null ? null : TemporalConversions.toTimestamp(oid, new String(value, encoding));
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

    public <TArray> TArray toArray(Class<TArray> arrayType, Oid oid, byte[] value) {
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
            case TEXT_ARRAY, CHAR_ARRAY, BPCHAR_ARRAY, VARCHAR_ARRAY -> StringConversions::toString;
            case NUMERIC_ARRAY, FLOAT4_ARRAY, FLOAT8_ARRAY -> NumericConversions::toBigDecimal;
            case TIMESTAMP_ARRAY, TIMESTAMPTZ_ARRAY -> TemporalConversions::toTimestamp;
            case TIMETZ_ARRAY, TIME_ARRAY -> TemporalConversions::toTime;
            case DATE_ARRAY -> TemporalConversions::toLocalDate;
            case BOOL_ARRAY -> BooleanConversions::toBoolean;
            case BYTEA_ARRAY -> (oide, svaluee) -> {
                byte[] first = BlobConversions.toBytes(oide, svaluee.substring(2));
                return parseHexBinary(new String(first, 1, first.length - 1, encoding));
            };
            default -> throw new IllegalStateException(STR."Unsupported array type: \{oid}");
        };
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<T> getConverter(Class<T> type) {
        var converter = (Converter<T>) typeToConverter.get(type);

        if (converter == null) {
            throw new IllegalArgumentException(STR."Unknown conversion target: \{type}");
        }

        return converter;
    }

    public <T> T toObject(Class<T> type, Oid oid, byte[] value) {
        var converter = getConverter(type);

        return value == null ? null
                             : converter.to(oid, new String(value, encoding));
    }

    private String fromObject(Object o) {
        return switch (o) {
            case null -> null;
            case Time time -> fromTime(time);
            case Timestamp timestamp -> fromTimestamp(timestamp);
            case LocalDate localDate -> fromLocalDate(localDate);
            case Date date -> fromDate(date);
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

    public Object toObject(Oid oid, byte[] value) {
        return switch (oid) {
            case null -> null;
            case TEXT, CHAR, BPCHAR, VARCHAR -> toString(oid, value);
            case INT2 -> toShort(oid, value);
            case INT4 -> toInteger(oid, value);
            case INT8 -> toLong(oid, value);
            case NUMERIC, FLOAT4, FLOAT8 -> toBigDecimal(oid, value);
            case BYTEA -> toBytes(oid, value);
            case DATE -> toLocalDate(oid, value);
            case TIMETZ, TIME -> toTime(oid, value);
            case TIMESTAMP, TIMESTAMPTZ -> toTimestamp(oid, value);
            case UUID -> UUID.fromString(toString(oid, value));
            case BOOL -> toBoolean(oid, value);
            case INT2_ARRAY, INT4_ARRAY, INT8_ARRAY, NUMERIC_ARRAY, FLOAT4_ARRAY, FLOAT8_ARRAY,
                TEXT_ARRAY, CHAR_ARRAY, BPCHAR_ARRAY, VARCHAR_ARRAY,
                TIMESTAMP_ARRAY, TIMESTAMPTZ_ARRAY, TIMETZ_ARRAY, TIME_ARRAY, BOOL_ARRAY -> toArray(Object[].class, oid, value);
            default -> toConvertible(oid, value);
        };
    }

    private Object toConvertible(Oid oid, byte[] value) {
        throw new IllegalStateException(STR."Unknown conversion source: \{oid}");
    }

    public Oid[] assumeTypes(Object... params) {
        var types = new Oid[params.length];

        for (int i = 0; i < params.length; i++) {
            switch (params[i]) {
                case Double _ -> types[i] = Oid.FLOAT8;
                case double[] _ -> types[i] = Oid.FLOAT8_ARRAY;
                case Float _ -> types[i] = Oid.FLOAT4;
                case float[] _ -> types[i] = Oid.FLOAT4_ARRAY;
                case Long _ -> types[i] = Oid.INT8;
                case long[] _ -> types[i] = Oid.INT8_ARRAY;
                case Integer _ -> types[i] = Oid.INT4;
                case int[] _ -> types[i] = Oid.INT4_ARRAY;
                case Short _ -> types[i] = Oid.INT2;
                case short[] _ -> types[i] = Oid.INT2_ARRAY;
                case Byte _ -> types[i] = Oid.INT2;
                case byte[] _ -> types[i] = Oid.BYTEA;
                case byte[][] _ -> types[i] = Oid.BYTEA_ARRAY;
                case BigInteger _ -> types[i] = Oid.NUMERIC;
                case BigInteger[] _ -> types[i] = Oid.NUMERIC_ARRAY;
                case BigDecimal _ -> types[i] = Oid.NUMERIC;
                case BigDecimal[] _ -> types[i] = Oid.NUMERIC_ARRAY;
                case Boolean _ -> types[i] = Oid.BOOL;
                case Boolean[] _ -> types[i] = Oid.BOOL_ARRAY;
                case CharSequence _, Character _ -> types[i] = Oid.VARCHAR;
                case Date _, Timestamp _, Instant _ -> types[i] = Oid.TIMESTAMP;
                case OffsetDateTime _ -> types[i] = Oid.TIMESTAMPTZ;
                case LocalDateTime _ -> types[i] = Oid.TIMESTAMP;
                case LocalDate _ -> types[i] = Oid.DATE;
                case Time _, OffsetTime _ -> types[i] = Oid.TIME;
                case LocalTime _ -> types[i] = Oid.TIMETZ;
                case UUID _ -> types[i] = Oid.UUID;
                case null, default -> types[i] = Oid.UNSPECIFIED;
            }
        }
        return types;
    }
}
