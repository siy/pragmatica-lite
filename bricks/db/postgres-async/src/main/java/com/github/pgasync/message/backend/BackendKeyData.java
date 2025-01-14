package com.github.pgasync.message.backend;

import com.github.pgasync.message.Message;

public record BackendKeyData(int pid, int cancelKey) implements Message {}
