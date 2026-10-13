
package com.mycompany.swingaplication;

// Represents one RSS news source, including its language.
public class RSSSource {

    private String name;      // Name of the RSS source
    private String url;       // URL of the RSS feed
    private String language;  // Language of the feed (e.g. "Romanian")

    // Constructor: initializes all three fields.
    public RSSSource(String name, String url, String language) {
        this.name = name;
        this.url = url;
        this.language = language;
    }

    // Returns the source name.
    public String getName() {
        return name;
    }

    // Returns the RSS feed URL.
    public String getUrl() {
        return url;
    }

    // Returns the feed language.
    public String getLanguage() {
        return language;
    }

    // Updates the feed language if needed.
    public void setLanguage(String language) {
        this.language = language;
    }

    // Displays the source name when the object is shown in a Swing component.
    @Override
    public String toString() {
        return name;
    }
}
