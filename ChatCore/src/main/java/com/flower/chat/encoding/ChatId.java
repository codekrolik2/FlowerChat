package com.flower.chat.encoding;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ChatId {
    public final List<String> userIds;

    public ChatId(List<String> userIds) {
        this.userIds = userIds.stream().sorted().toList();
    }

    public ChatId(String... userId) {
        this(Arrays.stream(userId).toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatId chatId = (ChatId) o;
        return Objects.equals(userIds, chatId.userIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userIds);
    }
}
