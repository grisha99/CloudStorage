package ru.grishenko.storage.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyEvent;
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
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    private static final Logger LOGGER = LogManager.getLogger(Controller.class.getName());

    private static final Path homePath = Paths.get(System.getProperty("user.home"));

//    @FXML
//    ContextMenu cmTF;

    @FXML
    Button remoteRefresh;

    @FXML
    Button localRefresh;

    @FXML
    MenuButton uploadMenuButton;

    @FXML
    MenuButton downloadMenuButton;

    @FXML
    TableView<FileInfo> localFilesView;

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

    private boolean upDiskChange;       // флаг изменен диск из списка, или програмно
    private boolean isDirCopy;

    public void menuExitClick(ActionEvent actionEvent) {
        sender.close();
        Platform.exit();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        LOGGER.log(Level.INFO, "Client Application Start");

        isAuthOK = false;
        upDiskChange = true;
        isDirCopy = false;

        TableInitializer.initTableView(localFilesView);
        LOGGER.log(Level.INFO, "Local table INIT");

        TableInitializer.initTableView(remoteFilesView);
        LOGGER.log(Level.INFO, "Remote table INIT");

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }

        uploadMenuButton.setDisable(true);

        updateFileList(homePath);

        sender = new NetworkSenderNetty((args) -> {

            if (args[0] instanceof Command) {
                Command cmd = (Command) args[0];
                switch (cmd.getType()) {
                    case AUTH_OK: {
                        isAuthOK = true;
                        authLayer.setVisible(false);
                        authLayer.setManaged(false);
                        workLayer.setDisable(false);
                        uploadMenuButton.setDisable(false);
                        remotePathField.setText(cmd.getArgs()[0]);
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        LOGGER.log(Level.INFO, "AUTH successful. Login: " + cmd.getArgs()[0]);
                        break;
                    }

                    case CD_OK: {
                        remotePathField.setText(cmd.getArgs()[0]/*.split("\\s")[0]*/);
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        break;
                    }

                    case FILE_OPERATION_OK: {
                        LOGGER.log(Level.INFO, "File operation successful");
                        sender.sendCommand(Command.generate(Command.CommandType.LIST));
                        break;
                    }

                    case MKDIR: {

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
                Path targetFileName;
//                if (Paths.get(fw.getFilePath()).getNameCount() > 1) {
                if (fw.getIsDeep()) {
                    targetFileName = getCurrentPath().resolve(fw.getFilePath());
                } else {
                    targetFileName = getCurrentPath().resolve(fw.getFileName());
                }


                try {
//                    if (fw.getType() == Command.CommandType.COPY) {

                    if (targetFileName.getFileName().toString().lastIndexOf(".") == -1) {
                        Files.createDirectories(targetFileName);
                    } else {
                        File file = new File(targetFileName.toString());
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
                            fos.getFD().sync();
                            fos.close();
                            fos = null;
                            LOGGER.log(Level.INFO, "Object \"" + fw.getFileName() + "\" received");
//                            updateFileList(getCurrentPath());
                        }
                    }
//                    }
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
            upDiskChange = false;
            disksBox.getSelectionModel().select(getDiskNumFromPath(path));
            upDiskChange = true;
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Refresh file list error, path: : " + path.toString());
            LOGGER.log(Level.ERROR, e);
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
        if (upDiskChange) {
            ComboBox<String> selectDisk = (ComboBox<String>) actionEvent.getSource();
            updateFileList(Paths.get(selectDisk.getSelectionModel().getSelectedItem()));
        }
    }

    public void localTableMouseDblClick(MouseEvent mouseEvent) {
        switchTransferAction(localFilesView);
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
//            cmTF.hide();
            if (mouseEvent.getClickCount() == 2) {
                switch (localFilesView.getSelectionModel().getSelectedItem().getType()) {
                    case DIRECTORY: {
                        Path path = Paths.get(localPathField.getText()).resolve(getSelectedFileName(true));
                        updateFileList(path);
                        break;
                    }
                    case UP_FOLDER: {
                        pathUp(null);
                        break;
                    }
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
        String pass = userPassField.getText().trim().equals("") ? null : userPassField.getText().trim();
        if (login != null && pass != null) {
            LOGGER.log(Level.INFO, "REGISTER info send");
            sender.sendCommand(Command.generate(Command.CommandType.REGISTER, login, pass));
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Укажите логин и пароль", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void remoteTableMouseDblClick(MouseEvent mouseEvent) {
        switchTransferAction(remoteFilesView);
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
//            cmTF.hide();
            if (mouseEvent.getClickCount() == 2) {
                switch (remoteFilesView.getSelectionModel().getSelectedItem().getType()) {
                    case DIRECTORY: {
                        sender.sendCommand(Command.generate(Command.CommandType.CD,
                                getSelectedFileName(false)));
                        break;
                    }
                    case UP_FOLDER: {
                        sender.sendCommand(Command.generate(Command.CommandType.CD_UP));
                        break;
                    }
                }
            }
        }
    }

    public void pathUpRemote(ActionEvent actionEvent) {
        sender.sendCommand(Command.generate(Command.CommandType.CD_UP));
    }

    public void requestPopup(ContextMenuEvent contextMenuEvent) {
//        if (localFilesView.isFocused()) {
//            cmTF.show(localFilesView, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
//        }
//        if (remoteFilesView.isFocused()) {
//            cmTF.show(remoteFilesView, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
//        }
    }

    public void createFileAction(ActionEvent actionEvent) throws IOException {
        String newFileName = showInputDialog("Создание файла", "Укажите имя файла", "Имя файла: ", null);
        if (newFileName != null) {
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
    }
    public void createFileRemoteAction(ActionEvent actionEvent) {
        String newFileName = showInputDialog("Создание файла", "Укажите имя файла", "Имя файла: ", null);
        if (newFileName != null) {
            LOGGER.log(Level.INFO, "File \"" + newFileName + "\" creat command send");
            sender.sendCommand(Command.generate(Command.CommandType.TOUCH, newFileName));
        }
    }

    public void createDirAction(ActionEvent actionEvent) throws IOException {
        String newDirName = showInputDialog("Создание каталога", "Укажите имя каталога", "Имя каталога: ", null);
        if (newDirName != null) {
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
    }

    public void createDirRemoteAction(ActionEvent actionEvent) {
        String newDirName = showInputDialog("Создание каталога", "Укажите имя каталога", "Имя каталога: ", null);
        if (newDirName != null) {
            LOGGER.log(Level.INFO, "Directory \"" + newDirName + "\" creat command send");
            sender.sendCommand(Command.generate(Command.CommandType.MKDIR, newDirName));
        }
    }

    public void renameAction(ActionEvent actionEvent) throws IOException {
        String oldFileName = getSelectedFileName(true);
        String newFileName = showInputDialog("Переименование...", "Укажите новое имя", "Новое имя: ", oldFileName);
        if (newFileName != null) {
            Path newFilePath = Paths.get(localPathField.getText()).resolve(newFileName);
            Path oldFilePath = Paths.get(localPathField.getText())
                    .resolve(oldFileName);
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
    }

    public void renameRemoteAction(ActionEvent actionEvent) {
        String oldFileName = getSelectedFileName(false);
        String newFileName = showInputDialog("Переименование...", "Укажите новое имя", "Новое имя: ", oldFileName);
        if (newFileName != null) {
            LOGGER.log(Level.INFO, "Rename command send");
            sender.sendCommand(Command.generate(Command.CommandType.REN, oldFileName, newFileName));
        }
    }

    public void deleteAction(ActionEvent actionEvent) {
        try {
            if (localFilesView.isFocused()) {
                Path toDeleteFile = Paths.get(localPathField.getText())
                        .resolve(getSelectedFileName(true));
                if (Files.exists(toDeleteFile)) {
                    if (Files.isDirectory(toDeleteFile)) {              // удаляем каталог
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить каталог \"" + toDeleteFile.getFileName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                        Optional<ButtonType> option = alert.showAndWait();
                        if (option.get() == ButtonType.OK) {
                            Files.walk(toDeleteFile)
                                    .sorted(Comparator.reverseOrder())
                                    .map(Path::toFile)
                                    .forEach(File::delete);
                            LOGGER.log(Level.INFO, "Directory tree \"" + toDeleteFile.getFileName() + "\" DELETE successful");
                        }
                    } else {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить файл \"" + toDeleteFile.getFileName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                        Optional<ButtonType> option = alert.showAndWait();
                        if (option.get() == ButtonType.OK) {
                            Files.delete(toDeleteFile);
                            LOGGER.log(Level.INFO, "Object \"" + toDeleteFile.getFileName() + "\" deleted successful");
                        }
                    }
                    updateFileList(Paths.get(localPathField.getText()));
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

    public void deleteRemoteAction(ActionEvent actionEvent) {
        String toDeleteFile = getSelectedFileName(false);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить файл \"" + toDeleteFile + "\"?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> option = alert.showAndWait();
        if (option.get() == ButtonType.OK) {
            LOGGER.log(Level.INFO, "Delete command send");
            sender.sendCommand(Command.generate(Command.CommandType.DEL, toDeleteFile));
        }
    }

    private String showInputDialog(String title, String header, String message, String defaultText) {
        TextInputDialog dialog = new TextInputDialog(defaultText);

        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(message);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public void uploadAction(ActionEvent actionEvent) throws IOException {
        if (localFilesView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
            Path filePath = Paths.get(localPathField.getText()).resolve(getSelectedFileName(true));
            fileUploader(Command.CommandType.COPY, filePath);
        } else if (localFilesView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
            directoryUploader(Command.CommandType.COPY);
        }
    }

    public void uploadDelAction(ActionEvent actionEvent) {
        Path filePath = Paths.get(localPathField.getText()).resolve(getSelectedFileName(true));
        fileUploader(Command.CommandType.MOVE, filePath);
        updateFileList(getCurrentPath());
    }

    private void fileUploader(Command.CommandType type, Path filePath) {
//        Path filePath = Paths.get(localPathField.getText()).
//                resolve(getSelectedFileName(true));
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            LOGGER.log(Level.INFO, "Upload transfer BEGIN, file name: " + filePath.getFileName());
            sender.sendFile(filePath, type);
        } else {
            LOGGER.log(Level.ERROR, "File for upload not selected");
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не выбран файл для загрузки", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void directoryUploader(Command.CommandType type) throws IOException {
        Path dirPath = Paths.get(localPathField.getText()).resolve(getSelectedFileName(true));
        Path currentRemoteDir = Paths.get(remotePathField.getText());
        Path copyROOT;
        if (currentRemoteDir.getNameCount() > 1) {
            copyROOT = currentRemoteDir.subpath(1, currentRemoteDir.getNameCount())/*.resolve(dirPath.getFileName())*/;
        } else {
            copyROOT = Paths.get("");
        }
        sender.sendCommand(Command.generate(Command.CommandType.SWITCH_FEED_BACK));
        try {

            Files.walk(dirPath).forEach(item -> {
                Path relativeRemotePath = item.subpath(dirPath.getParent().getNameCount(), item.getNameCount());
                if (Files.isDirectory(item)) {
                    sender.sendCommand(Command.generate(Command.CommandType.CD_ROOT));
                    sender.sendCommand(Command.generate(Command.CommandType.CD, copyROOT.toString()));
                    sender.sendCommand(Command.generate(Command.CommandType.MKDIR, relativeRemotePath.toString()));
                } else {
                    sender.sendCommand(Command.generate(Command.CommandType.CD_ROOT));
                    sender.sendCommand(Command.generate(Command.CommandType.CD, copyROOT.resolve(relativeRemotePath.getParent()).toString()));
                    fileUploader(Command.CommandType.COPY, item);
                }
            });
            LOGGER.log(Level.INFO, "Folder \"" + dirPath.getFileName() + "\" and all subfolder COPY to Server.");
        } finally {
            sender.sendCommand(Command.generate(Command.CommandType.CD_ROOT));
            sender.sendCommand(Command.generate(Command.CommandType.SWITCH_FEED_BACK));
            sender.sendCommand(Command.generate(Command.CommandType.CD, copyROOT.toString()));
        }
    }

    public void downloadAction(ActionEvent actionEvent) throws IOException {
        fileDownloader(Command.CommandType.COPY);
    }

    public void downloadDelAction(ActionEvent actionEvent) throws IOException {
        fileDownloader(Command.CommandType.MOVE);
    }

    private void fileDownloader(Command.CommandType type) throws IOException {
        if (remoteFilesView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.FILE) {
            if (remoteFilesView.getSelectionModel().getSelectedItem().getSize() > 0) {
                sender.sendCommand(Command.generate(type, getSelectedFileName(false)));
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
        if (remoteFilesView.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
            sender.sendCommand(Command.generate(type, getSelectedFileName(false)));
            LOGGER.log(Level.INFO, "Send command to download, Directory name: " + getSelectedFileName(false));
        }
    }

    public void localRefreshAction(ActionEvent actionEvent) {
            int row = localFilesView.getSelectionModel().getSelectedIndex();
            updateFileList(getCurrentPath());
            if (localFilesView.getItems().size() >= row) {
                localFilesView.getSelectionModel().select(row);
            } else {
                localFilesView.getSelectionModel().select(0);
            }
    }

    public void remoteRefreshAction(ActionEvent actionEvent) {
        sender.sendCommand(Command.generate(Command.CommandType.LIST));
    }

    public void toHomeDir(ActionEvent actionEvent) {
        updateFileList(homePath);
    }

    public int getDiskNumFromPath(Path path) {
        String rootDisk = path.toAbsolutePath().getRoot().toString();
        for (int i = 0; i < disksBox.getItems().size(); i++) {
            if (disksBox.getItems().get(i).equals(rootDisk)) {
                return i;
            }
        }
        return 1;
    }

    public void toRootDir(ActionEvent actionEvent) {
        Path rootPath = getCurrentPath().toAbsolutePath().getRoot();
        updateFileList(rootPath);
    }


    public void toRootDirRemote(ActionEvent actionEvent) {
        sender.sendCommand(Command.generate(Command.CommandType.CD_ROOT));
    }

    public void toHomeDirRemote(ActionEvent actionEvent) {
        //todo add functional for user home dir on remote
        sender.sendCommand(Command.generate(Command.CommandType.CD_ROOT));
    }


    public void onKeyRealisedLocalAction(KeyEvent keyEvent) throws IOException {
        switchTransferAction(localFilesView);
//        Files.walk(Paths.get("./cloud-storage-server")).forEach(System.out::println);
    }


    public void onKeyRealisedRemoteAction(KeyEvent keyEvent) {
        switchTransferAction(remoteFilesView);
    }


    private void switchTransferAction(TableView<FileInfo> table) {
        boolean isDisabled;
        if (table.getSelectionModel().getSelectedItem() == null) {
            isDisabled = true;
        } else {
            isDisabled = (table.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.UP_FOLDER || !isAuthOK);
        }
        if (table.getId().equals("localFilesView")) {
            uploadMenuButton.setDisable(isDisabled);
        }
        if (table.getId().equals("remoteFilesView")) {
            downloadMenuButton.setDisable(isDisabled);
        }
    }


}
