package com.vivo.chat;

import com.vivo.chat.encoding.ChatId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatIdTest {
    @Test
    public void test() {
        String user1 = "user1";
        String user2 = "user2";
        String user3 = "user3";

        ChatId chatId11 = new ChatId(user1, user2);
        ChatId chatId12 = new ChatId(user2, user1);

        assertEquals(chatId11.hashCode(), chatId12.hashCode());

        ChatId chatId21 = new ChatId(user1, user2, user3);
        ChatId chatId22 = new ChatId(user2, user1, user3);
        ChatId chatId23 = new ChatId(user3, user2, user1);

        assertEquals(chatId21.hashCode(), chatId22.hashCode());
        assertEquals(chatId21.hashCode(), chatId23.hashCode());
        assertEquals(chatId22.hashCode(), chatId23.hashCode());
    }
}
