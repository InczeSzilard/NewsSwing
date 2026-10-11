
package com.mycompany.swingaplication;

// Gson is used to convert JSON data into Java objects
import com.google.gson.Gson;

// FileReader is used to read the RSS.json file
import java.io.FileReader;

// IOException handles errors that occur when reading the file
import java.io.IOException;

public class RSSLoader {

    // Loads RSS source information from the RSS.json file
    // and returns it as an RSSparse object.
    public RSSparse loadSources() {

        // Create a Gson object to parse JSON data into Java objects.
        Gson gson = new Gson();

        try {

            // Open the RSS.json file for reading.
            // The file is searched for using the application's
            // current working directory.
            FileReader reader = new FileReader("RSS.json");

            // Convert the JSON content into an RSSparse object.
            // RSSparse must match the structure of the JSON data.
            RSSparse sources =
                    gson.fromJson(reader, RSSparse.class);

            // Close the file after reading its contents.
            reader.close();

            // Print a message confirming that the file was loaded.
            System.out.println("RSS.json loaded successfully!");

            // Check that the parsed object and its RSS source list
            // are not null before trying to access the list.
            if (sources != null && sources.getRssSources() != null) {

                // Print the number of RSS sources found in the file.
                System.out.println(
                        "Number of RSS sources: "
                        + sources.getRssSources().size()
                );
            }

            // Return the loaded RSS sources to the calling code.
            return sources;

        } catch (IOException e) {

            // Display an error message if the file cannot be opened
            // or another input/output error occurs.
            System.out.println("Could not open RSS.json!");

            // Print the error details to help identify the problem.
            e.printStackTrace();

            // Return null to indicate that loading failed.
            return null;
        }
    }
}
