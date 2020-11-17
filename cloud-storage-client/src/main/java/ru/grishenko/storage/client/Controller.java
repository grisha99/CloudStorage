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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.grishenko.storage.client.helper.Command;
import ru.grishenko.storage.client.helper.FileInfo;
import ru.grishenko.storage.client.helper.FileWrapper;
import ru.grishenko.storage.client.helper.TableInitializer;

import java.io.File;
import java.io.FileOutputStream;
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

    private static final Logger LOGGER = LogManager.getLogger(Controller.class.getName());

    @FXML
    ContextMenu cmTF;

    @FXML
    Button uploadButton;

    @FXML
    Button downloadButton;

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
    private FileOutputStream fos;

    public void menuExitClick(ActionEvent actionEvent) {
        sender.close();
        Platform.exit();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        LOGGER.log(Level.INFO, "Client Apploication Start");

        isAuthOK = false;

        localFilesView.getColumns().addAll(TableInitializer.getColumns());
        localFilesView.getSortOrder().add((localFilesView.getColumns()).get(0));
        LOGGER.log(Level.INFO, "Local table INIT");

        remoteFilesView.getColumns().addAll(TableInitializer.getColumns());
        remoteFilesView.getSortOrder().add((remoteFilesView.getColumns()).get(0));
        LOGGER.log(Level.INFO, "Remote table INIT");

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

            if (args[0] instanceof Command) {
                Command cmd = (Command) args[0];
                switch (cmd.getType()) {
                    case AUTH_OK: {
                        isAuthOK = true;
                        authLayer.setVisible(false);
                        authLayer.setManaged(false);
                        workLayer.setDisable(false);
                        centerPanel.setDisable(false);
                        remotePathField.setText(cmd.getArgs()[0]);
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        LOGGER.log(Level.INFO, "AUTH successful. Login: " + cmd.getArgs()[0]);
                        break;
                    }

                    case CD_OK: {
                        remotePathField.setText(cmd.getArgs()[0].split("\\s")[0]);
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        break;
                    }

                    case FILE_OPERATION_OK: {
                        LOGGER.log(Level.INFO, "File operation successful");
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        break;
                    }

                    case ERROR: {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, cmd.getArgs()[0], ButtonType.OK);
                            alert.showAndWait();
                        });
                        LOGGER.log(Level.ERROR, cmd.getArgs()[0]);
                        break;
                    }
                }
            }

            if (args[0] instanceof FileWrapper) {
                FileWrapper fw = (FileWrapper) args[0];
                try {
                    if (fw.getType() == Command.CommandType.COPY) {
                        File file = new File(getCurrentPath().resolve(fw.getFileName()).toString());
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        if (fos == null) {
                            fos = new FileOutputStream(file);

                        }
                        if (fos.getChannel().isOpen()) {
                            fos.write(fw.getBuffer(), 0, fw.getReadByte());
                        } else {
                            fos = new FileOutputStream(file);
                            fos.write(fw.getBuffer(), 0, fw.getReadByte());

                        }
                        if (fw.getCurrentPart() == fw.getParts()) {
                            LOGGER.log(Level.INFO, "Object \"" + fw.getFileName() + "\" received");
                            fos.close();
                            updateFileList(getCurrentPath());
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (args[0] instanceof List) {
                List list = (List)args[0];
                if ( list.size() > 0 && list.get(0) instanceof FileInfo) {
                    updateRemoteFileList((List<FileInfo>)list);
                    LOGGER.log(Level.INFO, "File List received");
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
            LOGGER.log(Level.ERROR, "Не удалось обновить список файлов по пути: " + path.toString());
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов по пути: " + path.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void updateRemoteFileList(List<FileInfo> fileInfoList) {
        remoteFilesView.getItems().clear();
        remoteFilesView.getItems().addAll(fileInfoList);
        remoteFilesView.sort();
    }

    public void pathUp(ActionEvent actionEvent) {
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
                if (!getSelectedFileName(true).equals("...") ||
                        localFilesView.getSelectionModel().getSelectedIndex() > 0) {
                    Path path = Paths.get(localPathField.getText()).resolve(getSelectedFileName(true));
                    if (Files.isDirectory(path)) {
                        updateFileList(path);
                    }
                } else {
                    pathUp(null);
                }
            }
        }
    }

    public String getSelectedFileName(boolean isLocal) {
        if (isLocal) {
            return localFilesView.getSelectionModel().getSelectedItem().getFileName();
        } else {
            return remoteFilesView.getSelectionModel().getSelectedItem().getFileName();
        }
    }

    public Path getCurrentPath() {
        return Paths.get(localPathField.getText());
    }

    public void sendAuthData(ActionEvent actionEvent) {
        LOGGER.log(Level.INFO, "AUTH info send");
        sender.sendCommand(Command.generate(Command.CommandType.AUTH, userNameField.getText(), userPassField.getText()));
    }

    public void sendRegisterAction(ActionEvent actionEvent) {
        String login = userNameField.getText().trim().equals("") ? null : userNameField.getText().trim();
        String pass = userPassField.getText().trim().equals("") ? null : userNameField.getText().trim();
        if (login != null && pass != null) {
            LOGGER.log(Level.INFO, "REGISTER info send");
            sender.sendCommand(Command.generate(Command.CommandType.REGISTER, login, pass));
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Укажите логин и пароль", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void remoteTableMouseDblClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            cmTF.hide();
            if (mouseEvent.getClickCount() == 2) {
                if (!getSelectedFileName(false).equals("...") ||
                        remoteFilesView.getSelectionModel().getSelectedIndex() > 0) {
                    if (remoteFilesView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
                        sender.sendCommand(Command.generate(Command.CommandType.CD,
                                getSelectedFileName(false)));
                    }
                    ;
                } else {
                    sender.sendCommand(Command.generate(Command.CommandType.UPCD));
                }
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
                    LOGGER.log(Level.INFO, "File \"" + newFileName + "\" created successful");
                } else {
                    LOGGER.log(Level.ERROR, "File with name \"" + newFileName + "\" already exist");
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
                    LOGGER.log(Level.INFO, "Directory \"" + newDirName + "\" created successful");
                } else {
                    LOGGER.log(Level.ERROR, "Directory with name \"" + newDirName + "\" already exist");
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
                        .resolve(getSelectedFileName(true));
                if (!Files.exists(newFilePath) && Files.exists(oldFilePath)) {
                    Files.move(oldFilePath, newFilePath);
                    LOGGER.log(Level.INFO, "Object renamed: Old name \"" + getSelectedFileName(true) + "\", New name \"" + newFileName + "\"");
                } else {
                    LOGGER.log(Level.ERROR, "Object with name \"" + newFileName + "\" already exist");
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Объекта с таким именем уже сущесвует", ButtonType.OK);
                    alert.showAndWait();
                }
                updateFileList(Paths.get(localPathField.getText()));

            }
            if (remoteFilesView.isFocused()) {
                String oldFileName = getSelectedFileName(false);
                sender.sendCommand(Command.generate(Command.CommandType.REN, oldFileName, newFileName));
            }
        }
    }

    public void deleteAction(ActionEvent actionEvent) {
        try {
            if (localFilesView.isFocused()) {
                Path toDeleteFile = Paths.get(localPathField.getText())
                        .resolve(getSelectedFileName(true));
                if (Files.exists(toDeleteFile)) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить файла \"" + toDeleteFile.getFileName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                    Optional<ButtonType> option = alert.showAndWait();
                    if (option.get() == ButtonType.OK) {
                        Files.delete(toDeleteFile);
                        LOGGER.log(Level.INFO, "Object \"" + toDeleteFile.getFileName() + "\" deleted successful");
                    }
                    updateFileList(Paths.get(localPathField.getText()));
                }
            }
            if (remoteFilesView.isFocused()) {
                String toDeleteFile = getSelectedFileName(false);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить файл \"" + toDeleteFile + "\"?", ButtonType.OK, ButtonType.CANCEL);
                Optional<ButtonType> option = alert.showAndWait();
                if (option.get() == ButtonType.OK) {
                    sender.sendCommand(Command.generate(Command.CommandType.DEL, toDeleteFile));
                }
            }
        } catch (NullPointerException e) {
            LOGGER.log(Level.ERROR, e);
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не выбран элемент для удаления", ButtonType.OK);
            alert.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, e);
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

    public void uploadAction(ActionEvent actionEvent) throws IOException {
        Path filePath = Paths.get(localPathField.getText()).
                resolve(getSelectedFileName(true));
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            LOGGER.log(Level.INFO, "Upload transfer BEGIN, file name: " + filePath.getFileName());
            sender.sendFile(filePath, Command.CommandType.COPY);

        } else {
            LOGGER.log(Level.ERROR, "File for upload not selected");
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не выбран файл для загрузки", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void downloadAction(ActionEvent actionEvent) throws IOException {
        if (remoteFilesView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
            if (remoteFilesView.getSelectionModel().getSelectedItem().getSize() > 0) {
                sender.sendCommand(Command.generate(Command.CommandType.COPY, getSelectedFileName(false)));
                LOGGER.log(Level.INFO, "Send command to download, file name: " + getSelectedFileName(false));
            } else {
                    Path newFilePath = Paths.get(localPathField.getText()).resolve(getSelectedFileName(false));
                    if (!Files.exists(newFilePath)) {
                        Files.createFile(newFilePath);
                    } else {
                        LOGGER.log(Level.INFO, "File with name \"" + getSelectedFileName(false) + "\" already exist");
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Файл с таким именем уже существует", ButtonType.OK);
                        alert.showAndWait();
                    }
                    updateFileList(Paths.get(localPathField.getText()));
            }
        }
    }
}
