package com.vivo.chat.chat;

import com.google.common.collect.ImmutableList;
import com.vivo.chat.encoding.ChatFeedVersion;
import com.vivo.chat.encoding.MessageId;
import com.vivo.chat.okhttp.ChatWebSocketClient;
import com.vivo.chat.okhttp.ConnectionStatusListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import okhttp3.WebSocket;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @FXML @Nullable TableColumn<UiChatMessage, String> messageColumn;

    final String server;
    final String myUsername;
    final List<String> otherUsernames;
    final HashMap<MessageId, Node> messageView = new HashMap<>();

    final ObservableList<UiChatMessage> chatMessages;

    @Nullable ChatWebSocketClient chatWebSocketClient;
    final ChatFeedVersion chatFeedVersion;
    @Nullable Stage stage;
    final AtomicBoolean connected = new AtomicBoolean(false);

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

    checkNotNull(messageColumn)
        .setCellFactory(
            new Callback<>() {
              @Override
              public TableCell<UiChatMessage, String> call(
                  TableColumn<UiChatMessage, String> tableColumn) {
                return new TableCell<>() {
                  protected void updateItem(String videoPath, boolean empty) {
                    super.updateItem(videoPath, empty);
                    int index = getIndex();
                    List<UiChatMessage> items = getTableView().getItems();
                    if (index >= 0 && index < items.size()) {
                        UiChatMessage message = items.get(index);
                        setGraphic(getView(message));
                    } else {
                        setGraphic(null);
                    }
                  }
                };
              }
            });

        List<String> usernames = ImmutableList.<String>builder().add(myUsername).addAll(otherUsernames).build();
        List<MessageId> versions = Stream.generate(() -> MessageId.MINIMAL_VERSION)
                .limit(usernames.size())
                .toList();

        chatFeedVersion = new ChatFeedVersion(
                usernames,
                versions);

        connect();
    }

    public Node getView(UiChatMessage message) {
        Node node;
        node = messageView.get(message.getMessageId());
        if (node == null) {
            VBox box = new VBox();
            if (message.getImage() != null) {
                InputStream inputStream = new ByteArrayInputStream(message.getImage());
                Image image = new Image(inputStream);
                ImageView imageView = new ImageView(image);
                imageView.fitHeightProperty().set(200);
                imageView.setPreserveRatio(true);
                box.getChildren().add(imageView);
            } else if (message.getVideo() != null) {
                try {
                    System.out.println("TempFile");
                    Path tmpFilePath = Files.createTempFile("vid_", ".mp4");
                    Files.write(tmpFilePath, message.getVideo());

                    Media media = new Media(tmpFilePath.toFile().toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    MediaView mediaView = new MediaView();
                    mediaView.setMediaPlayer(mediaPlayer);
                    Button playButton = new Button("Play");
                    Button pauseButton = new Button("Pause");
                    Button stopButton = new Button("Stop");
                    playButton.setOnAction(ev -> mediaPlayer.play());
                    pauseButton.setOnAction(ev -> mediaPlayer.pause());
                    stopButton.setOnAction(ev -> mediaPlayer.stop());

                    HBox controls = new HBox(10, playButton, pauseButton, stopButton);

                    box.getChildren().add(mediaView);
                    box.getChildren().add(controls);
                } catch (IOException e) {
                    box.getChildren().add(new Text("Can't show video " + e));
                }
            } else {
                box.getChildren().add(new Text(message.getMessage()));
            }
            messageView.put(message.getMessageId(), box);
            return box;
        }
        return node;
    }

    public void showNotConnected() {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Not connected. PLease reconnect.", ButtonType.OK);
        alert.showAndWait();
    }

    public void sendImage() throws IOException {
        if (!connected.get()) {
            showNotConnected();
            return;
        }

        File imageFile = openFile(true);
        if (imageFile != null) {
            byte[] bytes = Files.readAllBytes(imageFile.toPath());
            checkNotNull(chatWebSocketClient).sendImage(bytes);
        }
    }

    public void sendVideo() throws IOException {
        if (!connected.get()) {
            showNotConnected();
            return;
        }

        File videoFile = openFile(false);
        if (videoFile != null) {
            checkNotNull(chatWebSocketClient).sendVideo(Files.readAllBytes(videoFile.toPath()));
        }
    }

    public void sendMessage() {
        if (!connected.get()) {
            showNotConnected();
            return;
        }

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

    void addMessage(UiChatMessage msg) {
        Platform.runLater(() -> {
            chatMessages.add(msg);
            checkNotNull(chatTable).scrollTo(chatMessages.size() - 1);
            checkNotNull(chatTable).refresh();
        });
    }

    private ChatWebSocketClient getChatWebSocketClient(String myUsername, ChatFeedVersion chatFeedVersion,
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
                    addMessage(UiChatMessage.from(msg));
                }
        );
        chatWebSocketClient.addConnectionStatusListener(new ConnectionStatusListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                LOGGER.info("Connection onOpen");
                addMessage(UiChatMessage.systemMessage("Connection onOpen"));
                connected.set(true);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                LOGGER.info("Connection onClosing {} {}", code, reason);
                addMessage(UiChatMessage.systemMessage("Connection onClosing"));
                connected.set(false);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                LOGGER.info("Connection onClosed {} {}", code, reason);
                addMessage(UiChatMessage.systemMessage("Connection onClosed"));
                connected.set(false);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response) {
                LOGGER.info("Connection onFailure", t);
                addMessage(UiChatMessage.systemMessage("Connection onFailure"));
                connected.set(false);
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
