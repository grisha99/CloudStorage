package ru.grishenko.storage.client.helper;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TableInitializer {

    public static List<TableColumn<FileInfo, ?>> getColumns() {

        List<TableColumn<FileInfo, ?>> result = new ArrayList<>();

        TableColumn<FileInfo, String> fileTypeCol = new TableColumn<>("Тип");
        fileTypeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeCol.setPrefWidth(24);

        result.add(fileTypeCol);

        TableColumn<FileInfo, String> fileNameCol = new TableColumn<>("Имя");
        fileNameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameCol.setPrefWidth(250);

        result.add(fileNameCol);

        TableColumn<FileInfo, Long> fileSizeCol = new TableColumn<>("Размер");
        fileSizeCol.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));
        fileSizeCol.setPrefWidth(110);

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

        result.add(fileSizeCol);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileModifiedDate = new TableColumn<>("Дата изменения");
        fileModifiedDate.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getModified().format(dtf)));
        fileModifiedDate.setPrefWidth(120);

        result.add(fileModifiedDate);

        return result;
    }
}
