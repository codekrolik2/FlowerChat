package com.vivo.chat.okhttp;

import com.vivo.chat.encoding.ChatFeedVersion;
import com.vivo.chat.encoding.ByteBufferEncoding;
import com.vivo.chat.encoding.ClientChatMessage;
import com.vivo.chat.encoding.MessageType;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ChatWebSocketClient extends WebSocketListener {
    final static Logger LOGGER = LoggerFactory.getLogger(ChatWebSocketClient.class);

    final AtomicReference<WebSocket> webSocket = new AtomicReference<>(null);
    final String url;
    final String myUsername;
    final ChatFeedVersion chatFeedVersion;

    final ConcurrentLinkedQueue<ConnectionStatusListener> connectionStatusListeners;
    final ConcurrentLinkedQueue<ChatMessageListener> chatMessageListeners;

    public ChatWebSocketClient(String url, String myUsername, ChatFeedVersion chatFeedVersion) {
        this.url = url;
        this.myUsername = myUsername;
        this.chatFeedVersion = chatFeedVersion;

        connectionStatusListeners = new ConcurrentLinkedQueue<>();
        chatMessageListeners = new ConcurrentLinkedQueue<>();
    }

    // --------------------- CONNECTION MANAGEMENT ---------------------

    public void reconnect() {
        reconnect(1000, null);
    }

    public void reconnect(int code, @Nullable String reason) {
        connect(webSocket.get(), code, reason);
    }

    public void connect(@Nullable WebSocket oldSocket, int code, @Nullable String reason) {
        if (webSocket.compareAndSet(oldSocket, null)) {
            if (oldSocket != null) {
                oldSocket.close(code, reason);
            }

            Request request = new Request.Builder().url(url).build();
            WebSocket newWebSocket = HttpBase.newWebSocket(request, this);

            webSocket.set(newWebSocket);

            // initiate chat
            introduce();
        }
    }

    public void disconnect() {
        WebSocket ws = webSocket.get();
        if (ws != null) {
            try {
                ws.close(1000, "Goodbye");
            } catch(Exception e) {
                LOGGER.info("Failed to disconnect", e);
            }
        }
    }

    // --------------------- SEND MESSAGES ---------------------

    public void introduce() {
        WebSocket ws = webSocket.get();
        if (ws != null) {
            ByteBuffer introBuffer = ByteBufferEncoding.encodeIntroMessage(myUsername, chatFeedVersion);
            ws.send(ByteString.of(introBuffer));
        }
    }

    public void sendPing() {
        WebSocket ws = webSocket.get();
        if (ws != null) {
            ws.send(ByteString.decodeHex("ping"));
        }
    }

    public void sendText(String text) {
        WebSocket ws = webSocket.get();
        if (ws != null) {
            ws.send(text);
        }
    }

    public void sendTextAsBinaryFrame(String text) {
        WebSocket ws = webSocket.get();
        if (ws != null) {
            ByteBuffer combinedBuffer = ByteBufferEncoding.combineWithMessageType(MessageType.TEXT, text.getBytes(StandardCharsets.UTF_8));
            ws.send(ByteString.of(combinedBuffer));
        }
    }

    public void sendImage(byte[] image) {
        WebSocket ws = webSocket.get();
        if (ws != null) {
            ByteBuffer combinedBuffer = ByteBufferEncoding.combineWithMessageType(MessageType.IMAGE, image);
            ws.send(ByteString.of(combinedBuffer));
        }
    }

    public void sendVideo(byte[] video) {
        WebSocket ws = webSocket.get();
        if (ws != null) {
            ByteBuffer combinedBuffer = ByteBufferEncoding.combineWithMessageType(MessageType.VIDEO, video);
            ws.send(ByteString.of(combinedBuffer));
        }
    }

    // --------------------- LISTENER MANAGEMENT ---------------------

    public void addConnectionStatusListener(ConnectionStatusListener connectionStatusListener) {
        connectionStatusListeners.add(connectionStatusListener);
    }

    public void removeConnectionStatusListener(ConnectionStatusListener connectionStatusListener) {
        connectionStatusListeners.remove(connectionStatusListener);
    }

    public void addMessageListener(ChatMessageListener chatMessageListener) {
        chatMessageListeners.add(chatMessageListener);
    }

    public void removeMessageListener(ChatMessageListener chatMessageListener) {
        chatMessageListeners.remove(chatMessageListener);
    }

    // --------------------- LISTENER RECEIVE MESSAGES ---------------------

    public void onMessage(WebSocket webSocket, String text) {
        //We don't receive text frames from the server, even for text MessageType
        LOGGER.warn("Text Message received from server [" + text + "]");
    }

    public void onMessage(WebSocket webSocket, ByteString bytes) {
        ByteBuffer messageBuffer = bytes.asByteBuffer();
        Optional<ClientChatMessage> chatMessage = ByteBufferEncoding.tryDecodeMessage(messageBuffer);
        if (chatMessage.isEmpty()) {
            LOGGER.warn("Binary message of unknown format received from server");
        } else {
            for (ChatMessageListener chatMessageListener : chatMessageListeners) {
                chatMessageListener.onMessage(webSocket, chatMessage.get());
            }
        }
    }

    // --------------------- LISTENER CONNECTION STATUS ---------------------

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        for (ConnectionStatusListener connectionStatusListener : connectionStatusListeners) {
            connectionStatusListener.onOpen(webSocket, response);
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        for (ConnectionStatusListener connectionStatusListener : connectionStatusListeners) {
            connectionStatusListener.onClosing(webSocket, code, reason);
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        for (ConnectionStatusListener connectionStatusListener : connectionStatusListeners) {
            connectionStatusListener.onClosed(webSocket, code, reason);
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response) {
        for (ConnectionStatusListener connectionStatusListener : connectionStatusListeners) {
            connectionStatusListener.onFailure(webSocket, t, response);
        }
    }
}