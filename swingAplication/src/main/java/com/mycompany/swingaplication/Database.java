
package com.mycompany.swingaplication;

// Used to create and manage database connections
import java.sql.Connection;

// Used to establish a connection to the database
import java.sql.DriverManager;

// Used to handle database connection errors
import java.sql.SQLException;

public class Database {

    // Defines the URL used to connect to the SQLite database.
    // The database file is named "news.db".
    // SQLite creates the file if it does not already exist,
    // provided the parent directory is accessible.
    private static final String URL = "jdbc:sqlite:news.db";

    // Creates and returns a connection to the database.
    // This method is static, so it can be called using
    // Database.getConnection() without creating a Database object.
    // SQLException is thrown if the connection cannot be established.
    public static Connection getConnection() throws SQLException {

        // Open a connection to the SQLite database and return it.
        return DriverManager.getConnection(URL);
    }
}
