package ru.grishenko.storage.client.helper;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo implements Serializable {

    public enum FileType {
        FILE("F"), DIRECTORY("D");

        private String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    private String fileName;
    private FileType type;
    private long size;
    private LocalDateTime modified;

    public FileInfo() {
        this.fileName = "...";
        this.type = FileType.DIRECTORY;
        this.size = -1L;
        this.modified = LocalDateTime.now();

    }
    public FileInfo(Path path){

        try {
            this.fileName = path.getFileName().toString();
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;

            if (Files.isDirectory(path)) {
                this.size = -1L;
            } else {
                this.size = Files.size(path);
            }

            this.modified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
        } catch (IOException e) {
            throw new RuntimeException("Error create FileInfo from path \"" + path.toString() + "\"");
        }

    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

}
