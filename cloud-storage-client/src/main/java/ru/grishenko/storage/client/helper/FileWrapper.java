package ru.grishenko.storage.client.helper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileWrapper implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(FileWrapper.class.getName());

    private static final int BUFFER_SIZE = 2048;            // размер буфера в байтах

    private final Command.CommandType type;

    private final String fileName;
    private final int parts;                                // общее количество частей
    private int currentPart;                                // текущая часть
    private int readByte;                                   // прочитанные байты
    private final byte[] buffer = new byte[BUFFER_SIZE];    // буфер для записи/чтения

    public FileWrapper(Path fileAbsolutePath, Command.CommandType type) throws IOException {
        this.fileName = fileAbsolutePath.getFileName().toString();
        this.parts = (int) ((Files.size(fileAbsolutePath) + buffer.length - 1) / BUFFER_SIZE);
        this.type = type;
        LOGGER.log(Level.INFO, "FileWrapper created, FileName: \"" + this.fileName + "\", Parts: " + this.parts);
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setCurrentPart(int currentPart) {
        this.currentPart = currentPart;
    }

    public int getCurrentPart() {
        return currentPart;
    }

    public int getReadByte() {
        return readByte;
    }

    public void setReadByte(int readByte) {
        this.readByte = readByte;
    }

    public int getParts() {
        return parts;
    }

    public Command.CommandType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return String.format("File: %s; Parts: %d; Current part: %d; buffer size: %d bytes", fileName, parts, currentPart, readByte);
    }
}
