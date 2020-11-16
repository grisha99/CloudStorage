package ru.grishenko.storage.client.helper;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileWrapper implements Serializable {

    private static final int BUFFER_SIZE = 512; // размер буфера в байтах

    private final Command.CommandType type;

    private final String fileName;
    private final int parts;
    private int currentPart;
    private int readByte;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    public FileWrapper(Path fileAbsolutePath, Command.CommandType type) throws IOException {
        this.fileName = fileAbsolutePath.getFileName().toString();
        this.parts = (int) ((Files.size(fileAbsolutePath) + buffer.length) / BUFFER_SIZE);
        this.type = type;
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
        return String.format("Файл: %s; чайстей: %d; часть: %d; в буфере: %d", fileName, parts, currentPart, readByte);
    }
}
