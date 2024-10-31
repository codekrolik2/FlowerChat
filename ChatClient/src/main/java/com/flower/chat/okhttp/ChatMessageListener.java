package com.flower.chat.okhttp;

import com.flower.chat.encoding.ClientChatMessage;
import okhttp3.WebSocket;

public interface ChatMessageListener {
    void onMessage(WebSocket webSocket, ClientChatMessage clientChatMessage);
}
