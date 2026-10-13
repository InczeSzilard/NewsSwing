
package com.mycompany.swingaplication;

// Used to store dates and times, including timestamps from the database
import java.sql.Timestamp;

// Represents a single news article in the application
public class News {

    // Stores the title of the news article
    private String title;

    // Stores the article's description or summary
    private String description;

    // Stores the name of the RSS source
    private String source;

    // Stores the name of the article's author
    private String author;

    // Stores the name of the publisher
    private String publisher;

    // Stores the date and time when the article was published
    private Timestamp publishDate;

    // Stores the URL of the original article
    private String url;

    // Stores the date and time when the article record was created
    private Timestamp creationDate;

    // Stores the date and time when the article record was last modified
    private Timestamp lastModifiedDate;

    // Stores the language of the article
    private String language;

    // Stores the URL of the article's cover image
    private String coverImage;


    // Constructor: initializes a News object with all its information
    public News(
            String title,
            String description,
            String source,
            String autor,
            String publisher,
            Timestamp publishDate,
            String url,
            Timestamp creationDate,
            Timestamp lastModifiedDate,
            String language,
            String coverImage) {

        // Assign the received values to the object's fields
        this.title = title;
        this.description = description;
        this.source = source;

        // The parameter is named "autor", but its value is stored
        // in the field named "author".
        this.author = autor;

        this.publisher = publisher;
        this.publishDate = publishDate;
        this.url = url;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
        this.language = language;
        this.coverImage = coverImage;
    }

    // Returns the title of the article
    public String getTitle() {
        return title;
    }

    // Returns the article's description
    public String getDescription() {
        return description;
    }

    // Returns the name of the RSS source
    public String getSource() {
        return source;
    }

    // Returns the author's name
    public String getAutor() {
        return author;
    }

    // Returns the publisher's name
    public String getPublisher() {
        return publisher;
    }

    // Returns the publication date and time
    public Timestamp getPublishDate() {
        return publishDate;
    }

    // Returns the URL of the original article
    public String getUrl() {
        return url;
    }

    // Returns the record creation date and time
    public Timestamp getCreationDate() {
        return creationDate;
    }

    // Returns the last modification date and time
    public Timestamp getLastModifiedDate() {
        return lastModifiedDate;
    }

    // Returns the language of the article
    public String getLanguage() {
        return language;
    }

    // Returns the cover image URL
    public String getCoverImage() {
        return coverImage;
    }

    // Overrides the default toString() method.
    // When a News object is displayed as text, its title is shown
    // instead of the default object name and memory reference.
    @Override
    public String toString() {
        return title;
    }

   // Updates the language of the article
public void setLanguage(String language) {
    this.language = language;
}
}
