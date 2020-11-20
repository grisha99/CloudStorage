package ru.grishenko.storage.client.helper;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TableInitializer {

    public static List<TableColumn<FileInfo, ?>> getColumns() {

        List<TableColumn<FileInfo, ?>> result = new ArrayList<>();

        TableColumn<FileInfo, String> fileTypeCol = new TableColumn<>("Тип");
        fileTypeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeCol.setPrefWidth(24);

        fileTypeCol.setCellFactory(column -> {
            return new TableCell<FileInfo, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    if (item == null || empty) {
                        setStyle("");
                        setGraphic(null);
                    } else {
                        ImageView iv = new ImageView("images/file.png");
                        iv.setFitHeight(16);
                        iv.setFitWidth(16);
                        if (item.equals("U")) {
                            iv.setImage(new Image("images/up.png"));
                        }
                        if (item.equals("D")) {
                            iv.setImage(new Image("images/folder.png"));
                        }
                        setGraphic(iv);
                    }
                }
            };
        });


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
                        if (item == -2L) {
                            text = null;
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
        fileModifiedDate.setCellFactory(column -> {
            return new TableCell<FileInfo, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        if (item.equals("0001-01-01 00:00:00")) {           // не обображаем дату для элемента перехода на уровень выше
                            setText(null);
                        } else {
                            setText(item);
                        }
                    }

                }
            };
        });

        result.add(fileModifiedDate);

        return result;
    }

    public static void initTableView(TableView<FileInfo> table) {

        table.getColumns().addAll(getColumns());
        table.sortPolicyProperty().set(t -> {
            Comparator<FileInfo> comparator = (r1, r2) ->
                    r1.getType() == FileInfo.FileType.UP_FOLDER ? -1            // переход вверх сверху
                            : r2.getType() == FileInfo.FileType.UP_FOLDER ? 1   // переход вверх сверху
                            : t.getComparator() == null ? 0                     // нечего сортировать
                            : t.getComparator().compare(r1, r2);                // сортируем
            FXCollections.sort(t.getItems(), comparator);
            return true;
        });
        table.getSortOrder().add((table.getColumns()).get(0));

    }
}
