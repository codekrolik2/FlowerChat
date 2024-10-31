package com.flower.chat.data;

import com.flower.chat.encoding.ChatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

public class ChannelMessageListener implements MessageListener {
    public final Channel channel;

    public ChannelMessageListener(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void sendMessage(ChatMessage message) {
        //INFO: retain since channel.write() releases buffers that were sent (recursively for composite buffers)
        message.outgoingBuffer.retain();
        // We shallow copy (slice) in order to keep readerIndex intact
        ByteBuf sliced = message.outgoingBuffer.slice();
        channel.writeAndFlush(new BinaryWebSocketFrame(sliced));
    }

    @Override
    public void sendMessages(List<ChatMessage> messages) {
        //INFO: retain since channel.write() releases buffers that were sent (recursively for composite buffers)
        for (ChatMessage message : messages) {
            message.outgoingBuffer.retain();
            // We shallow copy (slice) the buffer in order to keep readerIndex intact
            ByteBuf sliced = message.outgoingBuffer.slice();
            channel.write(new BinaryWebSocketFrame(sliced));
        }
        channel.flush();
    }
}