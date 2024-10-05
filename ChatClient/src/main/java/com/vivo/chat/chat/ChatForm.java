package com.vivo.chat.chat;

import com.google.common.collect.ImmutableList;
import com.vivo.chat.encoding.ChatFeedVersion;
import com.vivo.chat.encoding.MessageId;
import com.vivo.chat.okhttp.ChatWebSocketClient;
import com.vivo.chat.okhttp.ConnectionStatusListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import okhttp3.WebSocket;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChatForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(ChatForm.class);
    final static long MAX_ATTACHMENT_SIZE = 10L*1024L*1024L;

    @FXML @Nullable TableView<UiChatMessage> chatTable;
    @FXML @Nullable TextField messageText;
    @FXML @Nullable CheckBox asTextFrameCheckBox;
    @FXML @Nullable TableColumn messageIdColumn;

    final String server;
    final String myUsername;
    final List<String> otherUsernames;

    final ObservableList<UiChatMessage> chatMessages;

    @Nullable ChatWebSocketClient chatWebSocketClient;
    final ChatFeedVersion chatFeedVersion;
    @Nullable Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public ChatForm(String server, String myUsername, List<String> otherUsernames) {
        this.server = server;
        this.myUsername = myUsername;
        this.otherUsernames = otherUsernames;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChatForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        chatMessages = FXCollections.observableArrayList();
        checkNotNull(chatTable).itemsProperty().set(new SortedList<>(chatMessages,
                (o1, o2) -> o1.getMessageId().compareTo(o2.getMessageId())));
        chatTable.getSortOrder().add(messageIdColumn);

        List<String> usernames = ImmutableList.<String>builder().add(myUsername).addAll(otherUsernames).build();
        List<MessageId> versions = Stream.generate(() -> MessageId.MINIMAL_VERSION)
                .limit(usernames.size())
                .toList();

        chatFeedVersion = new ChatFeedVersion(
                usernames,
                versions);

        connect();
    }

    public void sendImage() throws IOException {
        File imageFile = openFile(true);
        if (imageFile != null) {
            byte[] bytes = Files.readAllBytes(imageFile.toPath());
            checkNotNull(chatWebSocketClient).sendImage(bytes);
        }
    }

    public void sendVideo() throws IOException {
        File videoFile = openFile(false);
        if (videoFile != null) {
            checkNotNull(chatWebSocketClient).sendVideo(Files.readAllBytes(videoFile.toPath()));
        }
    }

    public void sendMessage() {
        String textMessage = checkNotNull(messageText).textProperty().get();
        if (!StringUtils.isBlank(textMessage)) {
            if (checkNotNull(asTextFrameCheckBox).selectedProperty().get()) {
                checkNotNull(chatWebSocketClient).sendText(textMessage);
            } else {
                checkNotNull(chatWebSocketClient).sendTextAsBinaryFrame(textMessage);
            }

            messageText.textProperty().set("");
        }
    }

    public void reconnect() {
        checkNotNull(chatWebSocketClient).reconnect();
    }

    public void disconnect() {
        checkNotNull(chatWebSocketClient).disconnect();
    }

    public void connect() {
        chatWebSocketClient = getChatWebSocketClient(myUsername, chatFeedVersion, server, chatMessages);
    }

    private static ChatWebSocketClient getChatWebSocketClient(String myUsername, ChatFeedVersion chatFeedVersion,
                                                              String server, List<UiChatMessage> chatMessages) {
        ChatWebSocketClient chatWebSocketClient = new ChatWebSocketClient(server, myUsername, chatFeedVersion);
        chatWebSocketClient.addMessageListener(
                (webSocket, msg) -> {
                    LOGGER.info("Message received: {}", msg);
                    AtomicReference<MessageId> msgIdRef = chatFeedVersion.userIdsAndVersions.get(msg.fromUsername);
                    while (true) {
                        MessageId oldMsgId = checkNotNull(msgIdRef).get();
                        if (checkNotNull(oldMsgId).compareTo(msg.messageId) < 0) {
                            if (msgIdRef.compareAndSet(oldMsgId, msg.messageId)) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    chatMessages.add(UiChatMessage.from(msg));
                }
        );
        chatWebSocketClient.addConnectionStatusListener(new ConnectionStatusListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                LOGGER.info("Connection onOpen");
                chatMessages.add(UiChatMessage.systemMessage("Connection onOpen"));
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                LOGGER.info("Connection onClosing {} {}", code, reason);
                chatMessages.add(UiChatMessage.systemMessage("Connection onClosing"));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                LOGGER.info("Connection onClosed {} {}", code, reason);
                chatMessages.add(UiChatMessage.systemMessage("Connection onClosed"));
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response) {
                LOGGER.info("Connection onFailure", t);
                chatMessages.add(UiChatMessage.systemMessage("Connection onFailure"));
                // TODO: auto-reconnect?
            }
        });
        chatWebSocketClient.reconnect();
        return chatWebSocketClient;
    }

    @Nullable public File openFile(boolean isImage) {
        try {
            FileChooser fileChooser = new FileChooser();
            if (isImage) {
                fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
                fileChooser.setTitle("Open Image");
            } else {
                fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Video",
                        "*.mp4", "*.avi", "*.mkv", "*.mov", "*.flv"));
                fileChooser.setTitle("Open Video");
            }

            File file = fileChooser.showOpenDialog(checkNotNull(stage));

            if (file != null) {
                long fileSize = file.length();
                if (file.length() > MAX_ATTACHMENT_SIZE) {
                    String message = String.format("Maximum attachment size [%d] bytes, file size [%d].", MAX_ATTACHMENT_SIZE, fileSize);
                    Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
                    LOGGER.error(message);
                    alert.showAndWait();

                    return null;
                }
            }
            return file;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error opening file: " + e, ButtonType.OK);
            LOGGER.error("Error opening file", e);
            alert.showAndWait();
            return null;
        }
    }

    public void formKeyRelease(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER){
            sendMessage();
        }
    }
}
