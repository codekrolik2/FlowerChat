package com.flower.chat.signIn;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import static com.google.common.base.Preconditions.checkNotNull;

public class SignInDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(SignInDialog.class);

    AtomicBoolean multiSignInProtection = new AtomicBoolean(false);

    @Nullable Stage stage;
    @Nullable @FXML public TextField serverTextField;
    @Nullable @FXML public TextField usernameTextField;
    @Nullable @FXML CheckBox rememberCheckBox;
    @Nullable @FXML Button signInButton;

    @Nullable String username;
    @Nullable String server;

    public SignInDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SignInDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        {
            //Set login preferences
            Preferences userPreferences = Preferences.userRoot();
            String email = userPreferences.get("chatUsername", "");
            checkNotNull(usernameTextField).textProperty().set(email);
            String server = userPreferences.get("chatServer", "");
            checkNotNull(serverTextField).textProperty().set(StringUtils.isBlank(server) ? "ws://127.0.0.1:8080/websocket" : server);

            String saveEmailStr = userPreferences.get("saveCreds", "False");
            boolean saveCreds;
            try {
                saveCreds = Boolean.parseBoolean(saveEmailStr);
            } catch (Exception e) {
                saveCreds = false;
            }
            checkNotNull(rememberCheckBox).selectedProperty().set(saveCreds);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void formKeyRelease(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER){
            signInAction();
        }
    }

    public void disableAllControls() {
        checkNotNull(signInButton).disableProperty().set(true);
        checkNotNull(serverTextField).disableProperty().set(true);
        checkNotNull(usernameTextField).disableProperty().set(true);
        checkNotNull(rememberCheckBox).disableProperty().set(true);
    }

    public void enableAllControls() {
        checkNotNull(signInButton).disableProperty().set(false);
        checkNotNull(serverTextField).disableProperty().set(false);
        checkNotNull(usernameTextField).disableProperty().set(false);
        checkNotNull(rememberCheckBox).disableProperty().set(false);
    }

    public void signInAction() {
        if (!multiSignInProtection.compareAndExchange(false, true)) {
            try {
                if (signIn()) {
                    disableAllControls();
                    checkNotNull(stage).close();
                }
            } finally {
                multiSignInProtection.compareAndExchange(true, false);
            }
        }
    }

    public boolean signIn() {
        username = checkNotNull(usernameTextField).textProperty().get().trim();
        server = checkNotNull(serverTextField).textProperty().get();
        boolean saveCreds = checkNotNull(rememberCheckBox).selectedProperty().get();

        if (StringUtils.isBlank(server)) {
            Alert alert = new Alert(Alert.AlertType.NONE, "Server can't be blank", ButtonType.OK);
            alert.showAndWait();
            return false;
        } else if (StringUtils.isBlank(username)) {
            Alert alert = new Alert(Alert.AlertType.NONE, "Username can't be blank", ButtonType.OK);
            alert.showAndWait();
            return false;
        } else {
            Preferences userPreferences = Preferences.userRoot();
            if (saveCreds) {
                userPreferences.put("chatUsername", username);
                userPreferences.put("chatServer", server);
                userPreferences.put("saveCreds", "True");
            } else {
                userPreferences.put("chatUsername", "");
                userPreferences.put("chatServer", "");
                userPreferences.put("saveCreds", "False");
            }
            return true;
        }
    }

    public String username() {
        return checkNotNull(username);
    }

    public String server() {
        return checkNotNull(server);
    }
}
