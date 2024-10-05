package com.vivo.chat.encoding;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ChatFeedVersion extends ChatId {
    public final Map<String, AtomicReference<MessageId>> userIdsAndVersions;

    public ChatFeedVersion(List<String> userIds, List<MessageId> versions) {
        super(userIds);
        ImmutableMap.Builder<String, AtomicReference<MessageId>> mapBuilder = ImmutableMap.builder();
        for (int i = 0; i < userIds.size(); i++) {
            mapBuilder.put(userIds.get(i), new AtomicReference<>(versions.get(i)));
        }

        this.userIdsAndVersions = mapBuilder.build();
    }
}
