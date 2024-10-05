package com.vivo.chat.encoding;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.vivo.chat.encoding.ByteBufferEncoding.getByteBufferContent;

public class ClientChatMessage {
    public final MessageType messageType;
    public final String fromUsername;
    public final MessageId messageId;

    public final ByteBuffer content;

    public ClientChatMessage(MessageType messageType, String fromUsername, MessageId messageId, ByteBuffer content) {
        this.messageType = messageType;
        this.fromUsername = fromUsername;
        this.messageId = messageId;
        this.content = content;
    }

    @Nullable public String contentAsString() {
        if (messageType == MessageType.TEXT) {
            byte[] contentBytes = getByteBufferContent(content);
            return new String(contentBytes, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public byte[] contentAsBytes() {
        return getByteBufferContent(content);
    }

    @Override
    public String toString() {
        String contentStr = Optional.ofNullable(contentAsString()).orElse("Binary");

        return String.format("user [%s] id [%s] type [%s] content [%s]",
                fromUsername,
                messageId,
                messageType,
                contentStr);
    }

    public String getUsername() {
        return fromUsername;
    }

    @Nullable public String getText() {
        return contentAsString();
    }
}
