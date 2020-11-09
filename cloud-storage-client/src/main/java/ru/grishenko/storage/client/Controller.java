package ru.grishenko.storage.client;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    TableView<FileInfo> localFilesView;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    TextField localPathField;

    @FXML
    TextField userNameField;

    @FXML
    PasswordField userPassField;

    @FXML
    VBox remoteSide;
    @FXML
    VBox authLayer;
    @FXML
    VBox workLayer;


    private boolean isAuthOK;

    private NetworkSenderNetty sender;
//    private NetworkSenderIO senderIO;

    public void menuExitClick(ActionEvent actionEvent) {
        Platform.exit();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        isAuthOK = false;

        TableColumn<FileInfo, String> fileTypeCol = new TableColumn<>("Тип");
        fileTypeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeCol.setPrefWidth(24);

        TableColumn<FileInfo, String> fileNameCol = new TableColumn<>("Имя");
        fileNameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameCol.setPrefWidth(300);

        TableColumn<FileInfo, Long> fileSizeCol = new TableColumn<>("Размер");
        fileSizeCol.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));
        fileSizeCol.setPrefWidth(150);

        fileSizeCol.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d byte", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileModifiedDate = new TableColumn<>("Дата изменения");
        fileModifiedDate.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getModified().format(dtf)));
        fileModifiedDate.setPrefWidth(120);

        localFilesView.getColumns().addAll(fileTypeCol, fileNameCol, fileSizeCol, fileModifiedDate);
        localFilesView.getSortOrder().add(fileTypeCol);

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);

        updateFileList(Paths.get("."));

        sender = new NetworkSenderNetty((args) -> {
            if (args[0] instanceof String ) {
                String answer = (String) args[0];
                System.out.println(answer);
                if (answer.startsWith("/AuthOK")) {
                    isAuthOK = true;
                    authLayer.setVisible(false);
                    workLayer.setVisible(true);
                }
            }
        });
//        senderIO = new NetworkSenderIO();


    }

    public void updateFileList(Path path) {

        try {
            localPathField.setText(path.normalize().toAbsolutePath().toString());
            localFilesView.getItems().clear();
            localFilesView.getItems().addAll(Files.list(path).map(FileInfo :: new).collect(Collectors.toList()));
            localFilesView.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов по пути: " + path.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnLocalUpAction(ActionEvent actionEvent) {
        Path parentPath = Paths.get(localPathField.getText()).getParent();
        if (parentPath != null) {
            updateFileList(parentPath);
        }
    }

    public void disksBoxAction(ActionEvent actionEvent) {
        ComboBox<String> selectDisk = (ComboBox<String>) actionEvent.getSource();
        updateFileList(Paths.get(selectDisk.getSelectionModel().getSelectedItem()));
    }

    public void localTableMouseDblClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            Path path = Paths.get(localPathField.getText()).resolve(localFilesView.getSelectionModel().getSelectedItem().getFileName());
            if (Files.isDirectory(path)) {
                updateFileList(path);
            }
        }
    }

    public String getSelectedFileName() {
        return localFilesView.getSelectionModel().getSelectedItem().getFileName();
    }

    public String getCurrnePath() {
        return localPathField.getText();
    }

    public void sendAuthData(ActionEvent actionEvent) {
        sender.sendCommand(String.format("/auth %s %s", userNameField.getText(), userPassField.getText()));
//        senderIO.sendMsgIO(String.format("/auth %s %s", userNameField.getText(), userPassField.getText()));
    }
}
