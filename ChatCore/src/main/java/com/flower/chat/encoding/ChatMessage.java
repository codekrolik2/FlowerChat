package com.flower.chat.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class ChatMessage {
    public final MessageType messageType;
    public final String fromUsername;
    public final MessageId messageId;

    /** Note: We need to `retain()` the buffer before `channel.write()`, or it will be released by `channel.write()` */
    public final ByteBuf outgoingBuffer;

    public ChatMessage(MessageType messageType, String fromUsername, MessageId messageId,
                       ByteBuf incomingBuffer, ByteBufAllocator allocator, boolean isTextFrame) {
        this.messageType = messageType;
        this.fromUsername = fromUsername;
        this.messageId = messageId;
        this.outgoingBuffer = ByteBufEncoding.prefixIncomingMessage(fromUsername, messageId, incomingBuffer, allocator, isTextFrame);
    }
}
