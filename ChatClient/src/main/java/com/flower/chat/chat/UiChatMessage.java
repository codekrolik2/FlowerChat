package com.flower.chat.chat;

import com.flower.chat.encoding.MessageId;
import com.flower.chat.encoding.ClientChatMessage;
import com.flower.chat.encoding.MessageType;

import javax.annotation.Nullable;

public interface UiChatMessage {

    class TextUiChatMessage implements UiChatMessage {
        final String username;
        final MessageId messageId;
        @Nullable final String message;
        @Nullable byte[] image;
        @Nullable byte[] video;

        public TextUiChatMessage(String username, MessageId messageId, @Nullable String message) {
            this.username = username;
            this.messageId = messageId;
            this.message = message;
            this.image = null;
            this.video = null;
        }

        public TextUiChatMessage(String username, MessageId messageId, byte[] content, boolean isImage) {
            this.username = username;
            this.messageId = messageId;
            if (isImage) {
                this.image = content;
                this.video = null;
                this.message = "Size: " + content.length;
            } else {
                this.image = null;
                this.video = content;
                this.message = "Size: " + content.length;
            }
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public MessageId getMessageId() {
            return messageId;
        }

        @Override
        @Nullable public String getMessage() {
            return message;
        }

        @Override
        @Nullable public byte[] getImage() {
            return image;
        }

        @Override
        @Nullable public byte[] getVideo() {
            return video;
        }
    }

    static UiChatMessage from(ClientChatMessage msg) {
        if (msg.messageType == MessageType.TEXT) {
            return new TextUiChatMessage(msg.getUsername(), msg.messageId, msg.getText());
        } else {
            return new TextUiChatMessage(msg.getUsername(), msg.messageId, msg.contentAsBytes(),
                    msg.messageType == MessageType.IMAGE);
        }
    }

    static UiChatMessage systemMessage(@Nullable String message) {
        return new TextUiChatMessage("", MessageId.now(), message);
    }

    String getUsername();
    MessageId getMessageId();
    @Nullable String getMessage();
    @Nullable byte[] getImage();
    @Nullable byte[] getVideo();
}
