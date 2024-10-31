package com.flower.chat.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class ByteBufEncoding {
    /** We don't discard incoming message, instead we're turning it into outgoing format by adding a prefix to it */
    public static ByteBuf prefixIncomingMessage(String username, MessageId messageId, ByteBuf incomingMessage,
                                                ByteBufAllocator allocator, boolean isTextFrame) {
        byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        int usernameLength = usernameBytes.length;

        int prefixLength = 4 + usernameLength + 8 + 4 + (isTextFrame ? 4 : 0);
        ByteBuf prefix = allocator.buffer(prefixLength);
        prefix.writeInt(usernameLength);
        prefix.writeBytes(usernameBytes);
        prefix.writeLong(messageId.timestamp);
        prefix.writeInt(messageId.serial);
        if (isTextFrame) {
            // Coming from TextFrame there is no message type code in buffer content
            prefix.writeInt(MessageType.TEXT.code);
        }

        CompositeByteBuf cbb = allocator.compositeDirectBuffer();
        //INFO: We better not forget leading boolean parameter here, or CompositeBuffer won't be readable
        cbb.addComponents(true, prefix, incomingMessage);

        return cbb;
    }

    /** We don't discard incoming message, instead we're turning it into outgoing format by adding a prefix to it */
    public static Optional<IntroMessage> tryDecodeIntroMessage(ByteBuf chatMessageBuffer) {
        // Message type (should be INTRO)
        int messageTypeCode = chatMessageBuffer.readInt();
        Optional<MessageType> messageTypeOpt = MessageType.tryGetMessageType(messageTypeCode);
        if (messageTypeOpt.isEmpty()) {
            return Optional.empty();
        }

        MessageType messageType = messageTypeOpt.get();
        if (messageType != MessageType.INTRO) {
            return Optional.empty();
        }

        // Read ChatFeedVersion
        String clientUsername = null;

        List<String> usernames = new ArrayList<>();
        List<MessageId> versions = new ArrayList<>();

        while (chatMessageBuffer.readableBytes() > 0) {
            int usernameLength = chatMessageBuffer.readInt();
            byte[] usernameBytes = new byte[usernameLength];
            chatMessageBuffer.readBytes(usernameBytes);
            String username = new String(usernameBytes, StandardCharsets.UTF_8);

            long timestamp = chatMessageBuffer.readLong();
            int serial = chatMessageBuffer.readInt();
            MessageId version = new MessageId(timestamp, serial);

            // By convention first username in the message is client's own username
            if (clientUsername == null) {
                clientUsername = username;
            }

            usernames.add(username);
            versions.add(version);
        }

        return Optional.of(new IntroMessage(checkNotNull(clientUsername), new ChatFeedVersion(usernames, versions)));
    }
}
