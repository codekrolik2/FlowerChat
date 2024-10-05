package com.vivo.chat.chat;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vivo.chat.MainApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenChatDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(OpenChatDialog.class);

    AtomicBoolean multiOpenProtection = new AtomicBoolean(false);

    @Nullable Stage stage;
    @Nullable @FXML TextField usernamesTextField;
    @Nullable @FXML Button startChatButton;

    @Nullable List<String> usersToChatWith;
    final String server;
    final String myUsername;
    final MainApp mainApp;

    public OpenChatDialog(String server, String myUsername, MainApp mainApp) {
        this.mainApp = mainApp;
        this.server = server;
        this.myUsername = myUsername;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("OpenChatDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void formKeyRelease(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER){
            startChatAction();
        }
    }

    public void startChatAction() {
        if (!multiOpenProtection.compareAndExchange(false, true)) {
            try {
                if (startChat()) {
                    disableAllControls();
                    checkNotNull(stage).close();
                    mainApp.showChatForm(checkNotNull(usersToChatWith));
                }
            } finally {
                multiOpenProtection.compareAndExchange(true, false);
            }
        }
    }

    boolean startChat() {
        String text = checkNotNull(usernamesTextField).textProperty().get();
        String[] parts = text.split(",");
        List<String> usernames = Arrays.stream(parts)
                .map(part -> part.trim())
                .filter(part -> !StringUtils.isBlank(part))
                .filter(part -> !part.equals(myUsername))
                .toList();
        if (usernames.size() <= 0) {
            Alert alert = new Alert(Alert.AlertType.NONE, "Add users other than yourself to the list", ButtonType.OK);
            alert.showAndWait();
            return false;
        }

        usersToChatWith = usernames;
        return true;
    }

    public void disableAllControls() {
        checkNotNull(usernamesTextField).disableProperty().set(true);
        checkNotNull(startChatButton).disableProperty().set(true);
    }
}
