package com.vivo.chat.encoding;

public class IntroMessage {
    public final String username;
    public final ChatFeedVersion chatFeedVersion;

    public IntroMessage(String username, ChatFeedVersion chatFeedVersion) {
        this.username = username;
        this.chatFeedVersion = chatFeedVersion;
    }
}
