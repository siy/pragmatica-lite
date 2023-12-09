package com.github.pgasync.message.backend;

import com.github.pgasync.message.Message;

public class BackendKeyData implements Message {

    private final int pid;
    private final int cancelKey;

    public BackendKeyData(int pid, int cancelKey) {
        this.pid = pid;
        this.cancelKey = cancelKey;
    }

    @Override
    public String toString() {
        return "BackendKeyData(" +
                "pid=" + pid +
                ", cancelKey=" + cancelKey +
                ')';
    }
}
