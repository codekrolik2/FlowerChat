package com.flower.chat.okhttp;

import okhttp3.WebSocket;

import javax.annotation.Nullable;

public interface ConnectionStatusListener {
    void onOpen(WebSocket webSocket, okhttp3.Response response);
    void onClosing(WebSocket webSocket, int code, String reason);
    void onClosed(WebSocket webSocket, int code, String reason);
    void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response);
}
