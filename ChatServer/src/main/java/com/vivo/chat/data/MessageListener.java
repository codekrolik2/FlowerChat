package com.vivo.chat.data;

import com.vivo.chat.encoding.ChatMessage;
import io.netty.buffer.ByteBuf;

import java.util.List;

public interface MessageListener {
    void sendMessage(ChatMessage message);
    void sendMessages(List<ChatMessage> messages);
}
