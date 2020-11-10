package ru.grishenko.storage.server.exception;

public class UserPassException extends Exception{
    public UserPassException() {
        super("Не указан пароль");
    }
}
