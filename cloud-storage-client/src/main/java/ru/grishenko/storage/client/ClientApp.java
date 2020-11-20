package ru.grishenko.storage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("Клиент облачного хранилища");
        primaryStage.setScene(new Scene(root, 1060, 600));

        primaryStage.setOnHidden(e -> controller.menuExitClick(null));

        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
