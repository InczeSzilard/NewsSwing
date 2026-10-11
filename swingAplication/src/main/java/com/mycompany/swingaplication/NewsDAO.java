
package com.mycompany.swingaplication;

// Used to connect to the database
import java.sql.Connection;

// Used to prepare and execute SQL queries with parameters
import java.sql.PreparedStatement;

// Used to handle SQL errors
import java.sql.SQLException;

public class NewsDAO {

    // Saves a news article into the database
    public void save(News news) {

        // SQL query to insert a news article into the news table.
        // INSERT OR IGNORE skips the insertion if a database
        // constraint is violated, for example a UNIQUE constraint.
        String sql = """
            INSERT OR IGNORE INTO news (
                title,
                description,
                source,
                author,
                publisher,
                publish_date,
                url,
                creation_date,
                last_modified_date,
                language,
                cover_image
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        // Open a database connection and prepare the SQL query.
        // try-with-resources automatically closes both resources
        // when the operation finishes.
        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            // Set the article title as the first SQL parameter
            statement.setString(1, news.getTitle());

            // Set the article description as the second parameter
            statement.setString(2, news.getDescription());

            // Set the name of the RSS source
            statement.setString(3, news.getSource());

            // Set the author's name
            statement.setString(4, news.getAutor());

            // Set the publisher's name
            statement.setString(5, news.getPublisher());

            // Set the article's publication date
            statement.setTimestamp(6, news.getPublishDate());

            // Set the article's URL
            statement.setString(7, news.getUrl());

            // Set the date when the article record was created
            statement.setTimestamp(8, news.getCreationDate());

            // Set the date when the article record was last modified
            statement.setTimestamp(9, news.getLastModifiedDate());

            // Set the language of the article
            statement.setString(10, news.getLanguage());

            // Set the URL of the article's cover image
            statement.setString(11, news.getCoverImage());

            // Execute the SQL query to save the article
            statement.executeUpdate();

        } catch (SQLException e) {

            // Display a message if saving the article fails
            System.out.println("Could not save news.");

            // Print details about the error to help with debugging
            e.printStackTrace();
        }
    }
}
