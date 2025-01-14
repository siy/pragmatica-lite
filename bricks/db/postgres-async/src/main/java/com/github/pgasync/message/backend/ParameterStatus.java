package com.github.pgasync.message.backend;

import com.github.pgasync.message.Message;

public class ParameterStatus implements Message {

    private final String name;
    private final String value;

    public ParameterStatus(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ParameterStatus(" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ')';
    }
}
