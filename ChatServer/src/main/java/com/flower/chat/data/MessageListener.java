package com.flower.chat.data;

import com.flower.chat.encoding.ChatMessage;

import java.util.List;

public interface MessageListener {
    void sendMessage(ChatMessage message);
    void sendMessages(List<ChatMessage> messages);
}
