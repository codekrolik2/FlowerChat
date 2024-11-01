package com.flower.chat.data;

import com.flower.chat.encoding.ChatFeedVersion;
import com.flower.chat.encoding.ChatId;
import com.flower.chat.encoding.MessageId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatCache {
    public final Map<ChatId, ChatBuffer> buffers;

    public ChatCache() {
        this.buffers = new ConcurrentHashMap<>();
    }

    public ChatBuffer getDialog(ChatId chatId) {
        return buffers.computeIfAbsent(chatId, key -> new ChatBuffer(key.userIds));
    }

    public void registerListener(ChatFeedVersion chatVersion, MessageListener listener) {
        getDialog(chatVersion).registerListener(chatVersion, listener);
    }

    public void unregisterListener(ChatId chatId, MessageListener listener) {
        getDialog(chatId).unregisterListener(listener);
    }

    public MessageId getMessageId(ChatId chatId, String fromUser) {
        return getDialog(chatId).getMessageId(fromUser);
    }
}
