package com.vivo.chat.encoding;

public class MessageId implements Comparable<MessageId> {
    public static final MessageId MINIMAL_VERSION = new MessageId(0, 0);

    public static MessageId now() {
        return new MessageId(System.currentTimeMillis(), 0);
    }

    public final long timestamp;
    public final int serial;

    public MessageId(long timestamp, int serial) {
        this.timestamp = timestamp;
        this.serial = serial;
    }

    /*public static final Comparator<MessageId> COMPARATOR = (id1, id2) -> {
        if (id1.timestamp != id2.timestamp) {
            return Long.compare(id1.timestamp, id2.timestamp);
        } else {
            return Integer.compare(id1.serial, id2.serial);
        }
    };*/

    @Override
    public String toString() {
        return String.format("{%d:%d}", timestamp, serial);
    }

    @Override
    public int compareTo(MessageId o) {
        if (this.timestamp != o.timestamp) {
            return Long.compare(this.timestamp, o.timestamp);
        } else {
            return Integer.compare(this.serial, o.serial);
        }
    }
}