package ru.grishenko.storage.client.helper;

import java.io.Serializable;

public class FileRequest implements Serializable {

    private String fileName;
    private Long fileSize;

    public FileRequest(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
