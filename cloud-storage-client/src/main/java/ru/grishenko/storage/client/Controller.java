package ru.grishenko.storage.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import ru.grishenko.storage.client.helper.Command;
import ru.grishenko.storage.client.helper.FileInfo;
import ru.grishenko.storage.client.helper.TableInitializer;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    ContextMenu cmTF;

    @FXML
    TableView<FileInfo> localFilesView;

    @FXML
    VBox centerPanel;

    @FXML
    TableView<FileInfo> remoteFilesView;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    TextField localPathField;

    @FXML
    TextField remotePathField;

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

    public void menuExitClick(ActionEvent actionEvent) {
        Platform.exit();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        isAuthOK = false;

        localFilesView.getColumns().addAll(TableInitializer.getColumns());
        localFilesView.getSortOrder().add((localFilesView.getColumns()).get(0));


        remoteFilesView.getColumns().addAll(TableInitializer.getColumns());
        remoteFilesView.getSortOrder().add((remoteFilesView.getColumns()).get(0));

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        String rootDisk = Paths.get(".").toAbsolutePath().getRoot().toString();
        for (int i = 0; i < disksBox.getItems().size(); i++) {
            if (disksBox.getItems().get(i).equals(rootDisk)) {
                disksBox.getSelectionModel().select(i);
                break;
            }
        }

        updateFileList(Paths.get("."));

        sender = new NetworkSenderNetty((args) -> {
            if (args[0] instanceof String ) {
                String answer = (String) args[0];
                System.out.println(answer);
            }

            if (args[0] instanceof Command) {
                Command cmd = (Command) args[0];
                switch (cmd.getType()) {
                    case AUTH_OK: {
                        isAuthOK = true;
                        authLayer.setVisible(false);
                        authLayer.setManaged(false);
                        workLayer.setDisable(false);
//                        centerPanel.setDisable(false);
                        remotePathField.setText(cmd.getArgs()[0]);
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        break;
                    }

                    case CD_OK: {
                        remotePathField.setText(cmd.getArgs()[0].split("\\s")[0]);
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        break;
                    }

                    case FILE_OPERATION_OK: {
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        break;
                    }

                    case ERROR: {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, cmd.getArgs()[0], ButtonType.OK);
                            alert.showAndWait();
                        });

                        break;
                    }
                }
            }

            if (args[0] instanceof List) {
                List list = (List)args[0];
                if ( list.get(0) instanceof FileInfo) {
                    updateRemoteFileList((List<FileInfo>)list);
                    System.out.println("получил лист файлинфо");
                }
            }
        });

    }

    public void updateFileList(Path path) {

        try {
            localPathField.setText(path.normalize().toAbsolutePath().toString());
            localFilesView.getItems().clear();
            if (Paths.get(localPathField.getText()).getParent() != null) {
                localFilesView.getItems().add(0, new FileInfo());
            }
            localFilesView.getItems().addAll(Files.list(path).map(FileInfo :: new).collect(Collectors.toList()));
            localFilesView.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов по пути: " + path.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void updateRemoteFileList(List<FileInfo> fileInfoList) {
        remoteFilesView.getItems().clear();
        remoteFilesView.getItems().addAll(fileInfoList);
        remoteFilesView.sort();
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
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            cmTF.hide();
            if (mouseEvent.getClickCount() == 2) {
                if (!localFilesView.getSelectionModel().getSelectedItem().getFileName().equals("...") ||
                        localFilesView.getSelectionModel().getSelectedIndex() > 0) {
                    Path path = Paths.get(localPathField.getText()).resolve(localFilesView.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        updateFileList(path);
                    }
                } else {
                    btnLocalUpAction(null);
                }
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
        sender.sendCommand(Command.generate(Command.CommandType.AUTH, userNameField.getText(), userPassField.getText()));
    }

    public void sendRegisterAction(ActionEvent actionEvent) {
        String login = userNameField.getText().trim().equals("") ? null : userNameField.getText().trim();
        String pass = userPassField.getText().trim().equals("") ? null : userNameField.getText().trim();
        if (login != null && pass != null) {
            sender.sendCommand(Command.generate(Command.CommandType.REGISTER, login, pass));
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Укажите логин и пароль", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void remoteTableMouseDblClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            if (!remoteFilesView.getSelectionModel().getSelectedItem().getFileName().equals("...") ||
                    remoteFilesView.getSelectionModel().getSelectedIndex() > 0) {
                if (remoteFilesView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
                    sender.sendCommand(Command.generate(Command.CommandType.CD,
                            remoteFilesView.getSelectionModel().getSelectedItem().getFileName()));
                };
            } else {
                sender.sendCommand(Command.generate(Command.CommandType.UPCD));
            }
        }
    }

    public void requestPopup(ContextMenuEvent contextMenuEvent) {
        if (localFilesView.isFocused()) {
            cmTF.show(localFilesView, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        }
        if (remoteFilesView.isFocused()) {
            cmTF.show(remoteFilesView, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        }
    }

    public void createFileAction(ActionEvent actionEvent) throws IOException {
        String newFileName = showInputDialog("Создание файла", "Укажите имя файла", "Имя файла: ");
        if (newFileName != null) {
            if (localFilesView.isFocused()) {
                Path newFilePath = Paths.get(localPathField.getText()).resolve(newFileName);
                if (!Files.exists(newFilePath)) {
                    Files.createFile(newFilePath);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Файл с таким именем уже существует", ButtonType.OK);
                    alert.showAndWait();
                }
                updateFileList(Paths.get(localPathField.getText()));

            }
            if (remoteFilesView.isFocused()) {
                sender.sendCommand(Command.generate(Command.CommandType.TOUCH, newFileName));
            }
        }
    }

    public void createDirAction(ActionEvent actionEvent) throws IOException {
        String newDirName = showInputDialog("Создание каталога", "Укажите имя каталога", "Имя каталога: ");
        if (newDirName != null) {
            if (localFilesView.isFocused()) {

                Path newFilePath = Paths.get(localPathField.getText()).resolve(newDirName);
                if (!Files.exists(newFilePath)) {
                    Files.createDirectory(newFilePath);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Каталог с таким именем уже существует", ButtonType.OK);
                    alert.showAndWait();
                }
                updateFileList(Paths.get(localPathField.getText()));

            }
            if (remoteFilesView.isFocused()) {
                sender.sendCommand(Command.generate(Command.CommandType.MKDIR, newDirName));
            }
        }
    }

    public void renameAction(ActionEvent actionEvent) throws IOException {
        String newFileName = showInputDialog("Переименование...", "Укажите новое имя", "Новое имя: ");
        if (newFileName != null) {
            if (localFilesView.isFocused()) {

                Path newFilePath = Paths.get(localPathField.getText()).resolve(newFileName);
                Path oldFilePath = Paths.get(localPathField.getText())
                        .resolve(localFilesView.getSelectionModel().getSelectedItem().getFileName());
                if (!Files.exists(newFilePath) && Files.exists(oldFilePath)) {
                    Files.move(oldFilePath, newFilePath);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Объекта с таким именем уже сущесвует", ButtonType.OK);
                    alert.showAndWait();
                }
                updateFileList(Paths.get(localPathField.getText()));

            }
            if (remoteFilesView.isFocused()) {
                String oldFileName = remoteFilesView.getSelectionModel().getSelectedItem().getFileName();
                sender.sendCommand(Command.generate(Command.CommandType.REN, oldFileName, newFileName));
            }
        }
    }

    public void deleteAction(ActionEvent actionEvent) throws IOException {
        try {
            if (localFilesView.isFocused()) {
                Path toDeleteFile = Paths.get(localPathField.getText())
                        .resolve(localFilesView.getSelectionModel().getSelectedItem().getFileName());
                if (Files.exists(toDeleteFile)) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить файла \"" + toDeleteFile.getFileName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                    Optional<ButtonType> option = alert.showAndWait();
                    if (option.get() == ButtonType.OK) {
                        Files.delete(toDeleteFile);
                    }
                    updateFileList(Paths.get(localPathField.getText()));
                }
            }
            if (remoteFilesView.isFocused()) {
                String toDeleteFile = remoteFilesView.getSelectionModel().getSelectedItem().getFileName();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить файл \"" + toDeleteFile + "\"?", ButtonType.OK);
                Optional<ButtonType> option = alert.showAndWait();
                if (option.get() == ButtonType.OK) {
                    sender.sendCommand(Command.generate(Command.CommandType.DEL, toDeleteFile));
                }
            }
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не выбран элемент для удаления", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private String showInputDialog(String title, String header, String message) {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(message);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public void uploadAction(ActionEvent actionEvent) {
//        FileInfo info = localFilesView.getSelectionModel().getSelectedItem();
//        if (info != null) {
//            sender.sendCommand(Command.generate(Command.CommandType.COPY, info.getFileName()));
//        }
    }

    public void downloadAction(ActionEvent actionEvent) {
        FileInfo info = remoteFilesView.getSelectionModel().getSelectedItem();
        if (info != null) {
            sender.sendCommand(Command.generate(Command.CommandType.COPY, info.getFileName()));
        }
    }
}
