package com.flower.chat.encoding;

import java.util.Optional;

public enum MessageType {
    INTRO(1),
    TEXT(2),
    IMAGE(3),
    VIDEO(4);

    public final int code;

    MessageType(int code) {
        this.code = code;
    }

    public static Optional<MessageType> tryGetMessageType(int code) {
        switch (code) {
            case 1: return Optional.of(INTRO);
            case 2: return Optional.of(TEXT);
            case 3: return Optional.of(IMAGE);
            case 4: return Optional.of(VIDEO);
            default: return Optional.empty();
        }
    }
}
