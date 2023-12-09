package com.github.pgasync.message.backend;

import com.github.pgasync.message.Message;

public class UnknownMessage implements Message {

    private final byte id;

    public UnknownMessage(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    @Override
    public String toString() {
        return "UnknownMessage(" +
                "id='" + (char) id + "')";
    }
}
