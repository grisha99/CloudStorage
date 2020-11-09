package ru.grishenko.storage.server.exception;

public class UserLoginException extends Exception{

    public UserLoginException(){
        super("Не указан логин и пароль пользователя");
    }
}
