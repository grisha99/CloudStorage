package ru.grishenko.storage.server.helper;

import ru.grishenko.storage.server.exception.UserLoginException;
import ru.grishenko.storage.server.exception.UserPassException;

public class AuthInfo {

    private String name;
    private String pass;

    public String getName() {
        return name;
    }

    public String getPass() {
        return pass;
    }

    public AuthInfo(String userString) throws UserLoginException, UserPassException {
        String [] tmp = userString.split(" ");
        if (tmp.length == 1) {
            throw new UserLoginException();
        }
        if (tmp.length == 2) {
            throw new UserPassException();
        }
        name = tmp[1];
        pass = tmp[2];
    }
}
