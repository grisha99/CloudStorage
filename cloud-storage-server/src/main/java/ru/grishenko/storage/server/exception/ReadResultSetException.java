package ru.grishenko.storage.server.exception;

import java.sql.SQLException;

public class ReadResultSetException extends SQLException {

    public ReadResultSetException() {
        super("Ошибка чтения данных");
    }
}
