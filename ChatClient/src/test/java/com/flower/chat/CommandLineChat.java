package com.flower.chat;

import com.flower.chat.encoding.ChatFeedVersion;
import com.flower.chat.encoding.MessageId;
import com.flower.chat.okhttp.ChatWebSocketClient;
import com.flower.chat.okhttp.ConnectionStatusListener;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CommandLineChat {
    final static Logger LOGGER = LoggerFactory.getLogger(CommandLineChat.class);
    static final String URL = System.getProperty("url", "ws://127.0.0.1:8080/websocket");

    public static void main(String[] args) throws IOException {
        ChatWebSocketClient chatWebSocketClient = getChatWebSocketClient();

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String msg = console.readLine();
            if (msg == null) {
                break;
            } else if ("bye".equals(msg.toLowerCase())) {
                chatWebSocketClient.disconnect();
                break;
            } else if ("ping".equals(msg.toLowerCase())) {
                chatWebSocketClient.sendPing();
            } else {
                chatWebSocketClient.sendText(msg);
            }
        }
    }

    private static ChatWebSocketClient getChatWebSocketClient() {
        ChatFeedVersion chatFeedVersion = new ChatFeedVersion(
                List.of("one", "two", "three", "john"),
                List.of(MessageId.MINIMAL_VERSION, MessageId.MINIMAL_VERSION, MessageId.MINIMAL_VERSION, MessageId.MINIMAL_VERSION));

        ChatWebSocketClient chatWebSocketClient = new ChatWebSocketClient(URL, "one", chatFeedVersion);
        chatWebSocketClient.addMessageListener(
            (webSocket, msg) -> {
                LOGGER.info("Message received: {}", msg);
            }
        );
        chatWebSocketClient.addConnectionStatusListener(new ConnectionStatusListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                LOGGER.info("Connection onOpen");
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                LOGGER.info("Connection onClosing {} {}", code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                LOGGER.info("Connection onClosed {} {}", code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response) {
                LOGGER.info("Connection onFailure", t);
                // TODO: auto-reconnect?
            }
        });
        chatWebSocketClient.reconnect();
        return chatWebSocketClient;
    }
}
