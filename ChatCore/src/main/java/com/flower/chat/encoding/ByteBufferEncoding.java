package com.flower.chat.encoding;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ByteBufferEncoding {
    /** We don't discard incoming message, instead we're turning it into outgoing format by adding a prefix to it */
    public static Optional<ClientChatMessage> tryDecodeMessage(ByteBuffer chatMessageBuffer) {
        int usernameLength = chatMessageBuffer.getInt();
        byte[] usernameBytes = new byte[usernameLength];
        chatMessageBuffer.get(usernameBytes);
        String username = new String(usernameBytes, StandardCharsets.UTF_8);

        long timestamp = chatMessageBuffer.getLong();
        int serial = chatMessageBuffer.getInt();
        MessageId messageId = new MessageId(timestamp, serial);

        // Message type (shouldn't be INTRO)
        int messageTypeCode = chatMessageBuffer.getInt();
        Optional<MessageType> messageTypeOpt = MessageType.tryGetMessageType(messageTypeCode);
        if (messageTypeOpt.isEmpty()) {
            return Optional.empty();
        }

        // The rest is content, we slice it from the header
        ByteBuffer content = chatMessageBuffer.slice();

        return Optional.of(new ClientChatMessage(messageTypeOpt.get(), username, messageId, content));
    }

    public static ByteBuffer encodeIntroMessage(String myUsername, ChatFeedVersion chatVersion) {
        MessageId myVersion = checkNotNull(chatVersion.userIdsAndVersions.get(myUsername)).get();

        List<byte[]> usernames = new ArrayList<>();
        List<MessageId> versions = new ArrayList<>();

        // My version comes first
        usernames.add(myUsername.getBytes(StandardCharsets.UTF_8));
        versions.add(myVersion);

        for (Map.Entry<String, AtomicReference<MessageId>> entry : chatVersion.userIdsAndVersions.entrySet()) {
            String username = entry.getKey();
            if (!username.equals(myUsername)) {
                MessageId messageId = entry.getValue().get();
                usernames.add(username.getBytes(StandardCharsets.UTF_8));
                versions.add(messageId);
            }
        }

        // Calculate message size first
        int bufferSize = 0;
        bufferSize += 4 + usernames.size() * 4;
        for (byte[] userId : usernames) {
            bufferSize += userId.length + 8 + 4;
        }

        // Allocate buffer of appropriate size
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.putInt(MessageType.INTRO.code);

        // Write structure to the buffer
        for (int i = 0; i < usernames.size(); i++) {
            byte[] userIdBytes = usernames.get(i);
            MessageId messageId = versions.get(i);

            buffer.putInt(userIdBytes.length);
            buffer.put(userIdBytes);
            buffer.putLong(messageId.timestamp);
            buffer.putInt(messageId.serial);
        }

        buffer.position(0);

        return buffer;
    }

    public static ByteBuffer combineWithMessageType(String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        return combineWithMessageType(MessageType.TEXT, textBytes);
    }

    public static ByteBuffer combineWithMessageType(MessageType messageType, byte[] bytes) {
        ByteBuffer messageBuf = ByteBuffer.allocate(4 + bytes.length);
        messageBuf.putInt(messageType.code);
        messageBuf.put(bytes);

        messageBuf.position(0);

        return messageBuf;
    }

    public static byte[] getByteBufferContent(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(0, bytes);
        return bytes;
    }
}
