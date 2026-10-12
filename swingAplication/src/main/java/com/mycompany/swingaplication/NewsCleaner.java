
package com.mycompany.swingaplication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class NewsCleaner {

    public NewsCleaner() {
        // Create the deletion-history table if it does not exist.
        try {
            Connection connection = Database.getConnection();
            Statement statement = connection.createStatement();

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS deleted_news ("
                    + "source TEXT NOT NULL, "
                    + "url TEXT NOT NULL, "
                    + "PRIMARY KEY (source, url))"
            );

            statement.close();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not initialize deletion history.", e);
        }
    }

    // Create a unique key for an article.
    public static String key(News article) {
        if (article == null) {
            return "";
        }

        return safe(article.getSource())
                + "|" + safe(article.getUrl());
    }

    // Convert null values to empty strings.
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // Delete one article and remember its URL so RSS refresh
    // will not download it again.
    public void deleteArticle(News article) throws Exception {
        if (article == null) {
            return;
        }

        Connection connection = Database.getConnection();

        PreparedStatement deleteStatement = null;
        PreparedStatement rememberStatement = null;

        try {
            connection.setAutoCommit(false);

            deleteStatement = connection.prepareStatement(
                    "DELETE FROM news WHERE source = ? AND url = ?"
            );

            deleteStatement.setString(1, safe(article.getSource()));
            deleteStatement.setString(2, safe(article.getUrl()));
            deleteStatement.executeUpdate();

            rememberStatement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO deleted_news (source, url) "
                    + "VALUES (?, ?)"
            );

            rememberStatement.setString(
                    1, safe(article.getSource()));

            rememberStatement.setString(
                    2, safe(article.getUrl()));

            rememberStatement.executeUpdate();

            connection.commit();

        } catch (Exception e) {
            connection.rollback();
            throw e;

        } finally {
            if (deleteStatement != null) {
                deleteStatement.close();
            }

            if (rememberStatement != null) {
                rememberStatement.close();
            }

            connection.setAutoCommit(true);
        }
    }

    // Load the keys of all articles previously deleted.
    public Set<String> loadDeletedKeys() throws Exception {
        Set<String> keys = new HashSet<String>();

        Connection connection = Database.getConnection();
        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();

            result = statement.executeQuery(
                    "SELECT source, url FROM deleted_news"
            );

            while (result.next()) {
                String source = safe(result.getString("source"));
                String url = safe(result.getString("url"));

                keys.add(source + "|" + url);
            }

        } finally {
            if (result != null) {
                result.close();
            }

            if (statement != null) {
                statement.close();
            }
        }

        return keys;
    }

    // Delete ALL saved news and ALL deletion records.
    // After this, Refresh can download the articles again.
    public void clearAll() throws Exception {
        Connection connection = Database.getConnection();
        Statement statement = null;

        try {
            statement = connection.createStatement();

            statement.executeUpdate("DELETE FROM news");
            statement.executeUpdate("DELETE FROM deleted_news");

        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }
}
