package ru.grishenko.storage.server.helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnect {

    private Connection connection;

    private static DBConnect dbConnectImpl;

    private DBConnect(){

        String jdbcURL = "jdbc:sqlite:cloud-storage-server/DB/serverDB.sqlite";
        try {
            connection = DriverManager.getConnection(jdbcURL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {

        return connection;
    }

    public static DBConnect getInstance() {

        if (dbConnectImpl == null) {
            dbConnectImpl = new DBConnect();
        }

        return dbConnectImpl;
    }
}
