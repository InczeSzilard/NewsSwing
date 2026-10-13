
package com.mycompany.swingaplication;

// Used to store dates and times for the event
import java.sql.Timestamp;

// Events extends News, meaning an event inherits
// the fields and methods defined in the News class.
public class Events extends News {

    // Stores the location where the event takes place
    private String location;

    // Constructor: creates an Events object with all the
    // news information plus the event's location.
    public Events(
            String title,
            String description,
            String source,
            String author,
            String publisher,
            Timestamp publishDate,
            String url,
            Timestamp creationDate,
            Timestamp lastModifiedDate,
            String language,
            String coverImage,
            String location) {

        // Call the News constructor to initialize the inherited
        // fields, such as title, description, dates, and URL.
        super(
                title,
                description,
                source,
                author,
                publisher,
                publishDate,
                url,
                creationDate,
                lastModifiedDate,
                language,
                coverImage
        );

        // Store the location specific to this event
        this.location = location;
    }

    // Returns the location where the event takes place
    public String getLocation() {
        return location;
    }
    //It changes the location
    public void setLocation(String location) {
    this.location = location;
}

    // Overrides the toString() method inherited from News.
    // When an Events object is displayed as text,
    // its title will be shown.
    @Override
    public String toString() {
        return getTitle();
    }
}