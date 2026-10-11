
package com.mycompany.swingaplication;

// Used to store multiple RSS sources in a list
import java.util.List;

// Represents the structure of the RSS.json configuration file
public class RSSparse {

    // Stores the list of RSS sources loaded from RSS.json.
    // Each RSSSource object contains information about one RSS feed.
    private List<RSSSource> rssSources;

    // Getter method that returns the list of RSS sources.
    // RSSLoader uses this method to access the loaded feeds.
    public List<RSSSource> getRssSources() {
        return rssSources;
    }
}
