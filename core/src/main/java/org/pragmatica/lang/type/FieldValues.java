package org.pragmatica.lang.type;

import java.util.List;

public interface FieldValues {
    List<?> values();
    int formatParameters(StringBuilder builder, int startIndex);
}
