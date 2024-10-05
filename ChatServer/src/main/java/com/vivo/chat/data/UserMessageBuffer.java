package com.vivo.chat.data;

import com.vivo.chat.encoding.ChatMessage;
import com.vivo.chat.encoding.MessageId;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserMessageBuffer {
    protected static final int DEFAULT_MAX_MESSAGES = 50;
    protected final ReentrantReadWriteLock lock;
    protected final TreeMap<MessageId, ChatMessage> messages;
    protected final ConcurrentLinkedQueue<MessageListener> listeners;
    final int maxMessages;

    public UserMessageBuffer() {
        this(DEFAULT_MAX_MESSAGES);
    }

    public UserMessageBuffer(int maxMessages) {
        this.lock = new ReentrantReadWriteLock();
        this.maxMessages = maxMessages;
        this.messages = new TreeMap<>();
        this.listeners = new ConcurrentLinkedQueue<>();
    }

    public void sendMessage(ChatMessage message) {
        lock.writeLock().lock();
        try {
            messages.put(message.messageId, message);
            if (messages.size() > maxMessages) {
                ChatMessage evictedMessage = messages.remove(messages.firstKey());
                checkNotNull(evictedMessage).outgoingBuffer.release();
            }
        } finally{
            lock.writeLock().unlock();
        }

        listeners.forEach(listener -> listener.sendMessage(message));
    }

    // ------------------- MESSAGE ID GENERATION -------------------

    final AtomicReference<MessageId> messageId = new AtomicReference<>(null);
    public MessageId getMessageId() {
        long currentTimestamp;
        int currentSerial;
        while (true) {
            currentTimestamp = System.currentTimeMillis();
            currentSerial = 0;
            MessageId oldMessageId = messageId.get();
            MessageId newMessageId;

            if (oldMessageId == null || oldMessageId.timestamp < currentTimestamp) {
                newMessageId = new MessageId(currentTimestamp, currentSerial);
            } else {
                newMessageId = new MessageId(oldMessageId.timestamp, oldMessageId.serial + 1);
            }
            if (messageId.compareAndSet(oldMessageId, newMessageId)) {
                return newMessageId;
            }
        }
    }

    // ------------------- LISTENERS -------------------

    public void registerListener(MessageId messageId, MessageListener listener) {
        listeners.add(listener);
        List<ChatMessage> messages = getSince(messageId, false);
        listener.sendMessages(messages);
    }

    public void unregisterListener(MessageListener listener) {
        listeners.remove(listener);
    }

    /** ATTENTION! The caller of this method must call ByteBuf::release() on each buffer */
    public List<ChatMessage> getSince(MessageId messageId, boolean inclusive) {
        lock.readLock().lock();
        try {
            return messages.tailMap(messageId, inclusive)
                    .values()
                    .stream()
                    .toList();
        } finally{
            lock.readLock().unlock();
        }
    }
}
