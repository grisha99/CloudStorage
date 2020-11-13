package ru.grishenko.storage.server.exception;

import java.sql.SQLException;

public class UserExistsException extends Exception {

    public UserExistsException() {
        super("Пользователья с таким логином уже существует");
    }
}
