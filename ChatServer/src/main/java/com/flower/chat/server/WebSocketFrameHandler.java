package com.flower.chat.server;

import com.flower.chat.encoding.ChatMessage;
import com.flower.chat.encoding.IntroMessage;
import com.flower.chat.encoding.MessageId;
import com.flower.chat.encoding.MessageType;
import com.flower.chat.data.ChannelMessageListener;
import com.flower.chat.data.ChatCache;
import com.flower.chat.encoding.ChatId;
import com.flower.chat.data.MessageListener;
import com.flower.chat.encoding.ByteBufEncoding;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import java.util.Optional;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    static final AttributeKey<IntroMessage> USERNAME_AND_CHAT_ID_ATTRIBUTE_KEY =
            AttributeKey.valueOf("username_and chat_id");
    static final AttributeKey<MessageListener> CHANNEL_MESSAGE_LISTENER_KEY =
            AttributeKey.valueOf("channel_message_listener");

    final ChatCache chatCache;
    final ByteBufAllocator allocator;

    public WebSocketFrameHandler(ChatCache chatCache, ByteBufAllocator allocator) {
        this.chatCache = chatCache;
        this.allocator = allocator;
    }

    void closeChannel(Channel channel) {
        channel.writeAndFlush(new CloseWebSocketFrame())
                .addListener(ChannelFutureListener.CLOSE);
    }

    void initChannel(Channel channel, WebSocketFrame frame) {
        // Channel is not tied to any chat or user, trying to get an introduction frame
        if (frame instanceof BinaryWebSocketFrame) {
            ByteBuf incomingMessage = frame.content();
            Optional<IntroMessage> introMessageOpt = ByteBufEncoding.tryDecodeIntroMessage(incomingMessage);
            if (introMessageOpt.isEmpty()) {
                // We didn't get introductory frame, something is wrong on the client - close channel
                closeChannel(channel);
            } else {
                IntroMessage introMessage = introMessageOpt.get();
                MessageListener messageListener = new ChannelMessageListener(channel);
                channel.attr(USERNAME_AND_CHAT_ID_ATTRIBUTE_KEY).set(introMessage);
                channel.attr(CHANNEL_MESSAGE_LISTENER_KEY).set(messageListener);

                chatCache.registerListener(introMessage.chatFeedVersion, messageListener);
            }
        } else {
            // We didn't get introductory frame (has to be binary), something is wrong on the client - close channel
            closeChannel(channel);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (channel.hasAttr(USERNAME_AND_CHAT_ID_ATTRIBUTE_KEY)) {
            ChatId chatId = channel.attr(USERNAME_AND_CHAT_ID_ATTRIBUTE_KEY).get().chatFeedVersion;
            MessageListener messageListener = channel.attr(CHANNEL_MESSAGE_LISTENER_KEY).get();

            chatCache.unregisterListener(chatId, messageListener);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // ping and pong frames already handled
        Channel channel = ctx.channel();

        if (!ctx.channel().hasAttr(USERNAME_AND_CHAT_ID_ATTRIBUTE_KEY)) {
            initChannel(channel, frame);
        } else {
            IntroMessage introMessage = channel.attr(USERNAME_AND_CHAT_ID_ATTRIBUTE_KEY).get();
            String fromUser = introMessage.username;
            ChatId chatId = introMessage.chatFeedVersion;

            if (frame instanceof TextWebSocketFrame) {
                ByteBuf message = frame.content();
                // INFO: incoming message will be released automatically after we leave this method,
                // so we need to retain it in order to keep it reusable as a part of composite buffer in the cache.
                message.retain();

                MessageId messageId = chatCache.getMessageId(chatId, fromUser);
                ChatMessage chatMessage = new ChatMessage(MessageType.TEXT, fromUser, messageId, message, allocator, true);

                chatCache.getDialog(chatId).sendMessage(fromUser, chatMessage);
            } else if (frame instanceof BinaryWebSocketFrame) {
                ByteBuf message = frame.content();
                Optional<MessageType> messageType = MessageType.tryGetMessageType(message.getInt(0));
                if (messageType.isPresent()) {
                    // INFO: incoming message will be released automatically after we leave this method,
                    // so we need to retain it in order to keep it reusable as a part of composite buffer in the cache.
                    message.retain();

                    MessageId messageId = chatCache.getMessageId(chatId, fromUser);
                    ChatMessage chatMessage = new ChatMessage(messageType.get(), fromUser, messageId, message, allocator, false);

                    chatCache.getDialog(chatId).sendMessage(fromUser, chatMessage);
                }
            } else {
                String message = "unsupported frame type: " + frame.getClass().getName();
                throw new UnsupportedOperationException(message);
            }
        }
    }
}
