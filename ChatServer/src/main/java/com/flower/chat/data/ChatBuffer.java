package com.flower.chat.data;

import com.flower.chat.encoding.ChatFeedVersion;
import com.flower.chat.encoding.ChatMessage;
import com.flower.chat.encoding.MessageId;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChatBuffer {
    public final Map<String, UserMessageBuffer> userMessageBuffers;

    public ChatBuffer(List<String> userIds) {
        ImmutableMap.Builder<String, UserMessageBuffer> mapBuilder = ImmutableMap.builder();
        userIds.forEach(userId -> mapBuilder.put(userId, new UserMessageBuffer()));
        userMessageBuffers = mapBuilder.build();
    }

    public void sendMessage(String userId, ChatMessage chatMessage) {
        checkNotNull(userMessageBuffers.get(userId))
            .sendMessage(chatMessage);
    }

    public void registerListener(ChatFeedVersion chatFeedVersion, MessageListener listener) {
        for (int i = 0; i < chatFeedVersion.userIds.size(); i++) {
            String userId = chatFeedVersion.userIds.get(i);
            AtomicReference<MessageId> version = chatFeedVersion.userIdsAndVersions.get(userId);
            checkNotNull(userMessageBuffers.get(userId))
                .registerListener(version != null ? checkNotNull(version.get()) : MessageId.MINIMAL_VERSION, listener);
        }
    }

    public void unregisterListener(MessageListener listener) {
        userMessageBuffers.values().forEach(userMessageBuffer -> userMessageBuffer.unregisterListener(listener));
    }

    public MessageId getMessageId(String fromUser) {
        return checkNotNull(userMessageBuffers.get(fromUser)).getMessageId();
    }
}
