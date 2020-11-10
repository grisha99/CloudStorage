package ru.grishenko.storage.server;

public class UserDAO {

    public boolean getUser(String login, String pass) {
        if (login.equals("login1") && pass.equals("123")) {
            return true;
        }
        return false;
    }
}
