package ru.grishenko.storage.server.helper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.grishenko.storage.server.ServerApp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnect {

    private static final Logger LOGGER = LogManager.getLogger(ServerApp.class.getName());

    private Connection connection;

    private static DBConnect dbConnectImpl;

    private DBConnect(){

//        String jdbcURL = "jdbc:sqlite:cloud-storage-server/DB/serverDB.sqlite";
        String jdbcURL = "jdbc:sqlite::resource:serverDB.sqlite";
        try {
            connection = DriverManager.getConnection(jdbcURL);
            LOGGER.log(Level.INFO, "DB connected successful");
        } catch (SQLException e) {
            LOGGER.log(Level.ERROR, e.getMessage());
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
