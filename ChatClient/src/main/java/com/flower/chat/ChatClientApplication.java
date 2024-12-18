package com.flower.chat;

import com.flower.chat.okhttp.HttpBase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatClientApplication extends Application {
    /**
     * Don't use this method directly, use ChatClientLauncher.
     * For whatever reason, running this directly will fail with an error.
     */
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage mainStage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ChatClientApplication.class.getResource("MainApp.fxml"));
            Parent rootNode = fxmlLoader.load();

            MainApp mainApp = fxmlLoader.getController();
            mainApp.setMainStage(mainStage);

            Scene mainScene = new Scene(rootNode, 1024, 768);

            //Close all threads when we close JavaFX windows.
            mainStage.setOnHidden(event -> {
                // TODO: close all tabs / clients
                // Shutdown Netty
                Platform.exit();
                HttpBase.shutdownHttp();
            });

            mainStage.setOnShown(event -> {
                mainApp.showSignInDialog(event, null);
            });

            mainStage.setTitle("Chat Client");
            mainStage.setScene(mainScene);
            mainStage.setResizable(true);
            mainStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
