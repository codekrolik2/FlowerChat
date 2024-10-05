package com.vivo.chat.okhttp;

import com.vivo.chat.encoding.ClientChatMessage;
import okhttp3.WebSocket;

public interface ChatMessageListener {
    void onMessage(WebSocket webSocket, ClientChatMessage clientChatMessage);
}
