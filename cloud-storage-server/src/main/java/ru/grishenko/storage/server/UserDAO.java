package ru.grishenko.storage.server;

import ru.grishenko.storage.server.exception.DBConnectException;
import ru.grishenko.storage.server.exception.ReadResultSetException;
import ru.grishenko.storage.server.exception.UserExistsException;
import ru.grishenko.storage.server.helper.DBConnect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    PreparedStatement ps;

    public boolean checkUser(String login, String pass) throws SQLException {
//        PreparedStatement ps;
        ResultSet rs;
        try {
            try {
                ps = DBConnect.getInstance().getConnection()
                        .prepareStatement("SELECT login, pass FROM `users` WHERE (`login` = ? and `pass` = ?);");
                ps.setString(1, login);
                ps.setString(2, pass);
                rs = ps.executeQuery();
            } catch (SQLException e) {
                throw new DBConnectException();
            }
            try {
                return rs.next();
            } catch (SQLException e) {
                throw new ReadResultSetException();
            }
        } finally {
            ps.close();
        }

    }

    public boolean registerUser(String login, String pass) throws SQLException, UserExistsException {
        ResultSet rs;
        try {
            try {
                ps = DBConnect.getInstance().getConnection()
                        .prepareStatement("SELECT login FROM `users` WHERE (`login` = ?);");
                ps.setString(1, login);
                rs = ps.executeQuery();
                if (rs.next()) {
                    throw new UserExistsException();
                }

                ps = DBConnect.getInstance().getConnection()
                        .prepareStatement("INSERT INTO users (`login`, `pass`) VALUES (?, ?)");
                ps.setString(1, login);
                ps.setString(2, pass);
                return ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

        } finally {
            ps.close();
        }
    }


}
