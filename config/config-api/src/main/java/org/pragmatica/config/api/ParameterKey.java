package org.pragmatica.config.api;

public record ParameterKey(String key) {
    static ParameterKey key(String key) {
        return new ParameterKey(key);
    }

    public ParameterKey withPrefix(String prefix) {
        if (prefix.isBlank()) {
            return this;
        }

        var trimmed = prefix.trim();
        return new ParameterKey(trimmed.endsWith(".")
                                ? STR."\{trimmed}\{key()}"
                                : STR."\{trimmed}.\{key()}");
    }
}
