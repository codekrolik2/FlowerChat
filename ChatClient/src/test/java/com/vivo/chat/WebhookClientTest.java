package com.vivo.chat;

import com.vivo.chat.encoding.ChatFeedVersion;
import com.vivo.chat.encoding.MessageId;
import com.vivo.chat.okhttp.ChatWebSocketClient;
import org.junit.jupiter.api.Test;

import java.util.List;

public class WebhookClientTest {
    static final String URL = System.getProperty("url", "ws://127.0.0.1:8080/websocket");

    @Test
    public void testOkHttp() throws Exception {
        ChatWebSocketClient chatWebSocketClient = new ChatWebSocketClient(URL, "denver", new ChatFeedVersion(
                List.of("ont", "denver"),
                List.of(MessageId.MINIMAL_VERSION, MessageId.MINIMAL_VERSION)));

        chatWebSocketClient.reconnect();

        chatWebSocketClient.introduce();

        Thread.sleep(100000);
    }
}
