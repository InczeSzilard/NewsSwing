
package com.mycompany.swingaplication;

// Import Swing utilities for safely creating the GUI
// and performing background tasks.
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

// Import collections used to store news articles and deleted article keys.
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main class of the news application.
 *
 * This class starts the graphical interface, connects the Refresh button
 * to the news-loading process, and loads news from configured RSS sources.
 *
 * It also saves downloaded articles to the database and prevents articles
 * previously deleted by the user from being loaded again.
 */
public class SwingAplication {

    // Main method: the entry point of the Java application.
    public static void main(String[] args) {

        // Create and display Swing components on the Event Dispatch Thread.
        // This is the recommended thread for creating and updating Swing GUIs.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // Create the main graphical user interface.
                final NewsGUI gui = new NewsGUI();

                // Connect the Refresh button to the RSS loading process.
                // When the user clicks Refresh, loadNews() is executed.
                gui.setRefreshAction(new NewsGUI.RefreshHandler() {
                    public void refresh() {
                        loadNews(gui);
                    }
                });

                // Display the application window.
                gui.setVisible(true);

                // Automatically load news when the application starts.
                loadNews(gui);
            }
        });
    }

    // ------------------------------------------------------------
    // LOAD NEWS FROM RSS SOURCES
    // ------------------------------------------------------------

    /**
     * Loads news articles from the configured RSS sources.
     *
     * A SwingWorker is used so that database operations and RSS downloads
     * run in the background without freezing the graphical interface.
     *
     *   the graphical interface that displays the news
     */
    private static void loadNews(final NewsGUI gui) {

        // Disable relevant controls and display a loading message.
        gui.setLoading(true, "Loading news...");

        // SwingWorker separates background processing from GUI updates.
        // List<News> is the background task's result type.
        // String is the type of progress messages sent to the GUI.
        SwingWorker<List<News>, String> worker =
                new SwingWorker<List<News>, String>() {

            // ----------------------------------------------------
            // BACKGROUND TASK
            // ----------------------------------------------------

            // Executes database operations and RSS downloads in the background.
            // This method must not directly update Swing components.
            protected List<News> doInBackground() {

                // Stores all articles successfully collected from RSS sources.
                List<News> allNews = new ArrayList<News>();

                try {
                    // Establish or initialize the database connection.
                    Database.getConnection();

                    // Send a progress message to process().
                    publish("Database connection successful.");

                    // Create a helper for managing deleted articles.
                    NewsCleaner cleaner = new NewsCleaner();

                    // Load the unique keys of articles previously deleted
                    // by the user. These articles should not reappear
                    // during subsequent RSS refreshes.
                    Set<String> deletedKeys =
                            cleaner.loadDeletedKeys();

                    // Create the RSS configuration loader.
                    RSSLoader loader = new RSSLoader();

                    // Read the configured RSS sources.
                    RSSparse sources = loader.loadSources();

                    // Stop if the configuration or source list is missing.
                    if (sources == null
                            || sources.getRssSources() == null) {

                        // Inform the user that no sources are configured.
                        publish("No RSS sources found.");

                        // Return an empty collection of articles.
                        return allNews;
                    }

                    // Create the service responsible for downloading
                    // and parsing RSS feeds.
                    RSSService rssService = new RSSService();

                    // Create the database access object used to save articles.
                    NewsDAO newsDAO = new NewsDAO();

                    // ------------------------------------------------
                    // PROCESS EACH RSS SOURCE
                    // ------------------------------------------------

                    // Download articles from every configured RSS source.
                    for (RSSSource source : sources.getRssSources()) {

                        // Display the name of the source currently being loaded.
                        publish("Loading: " + source.getName());

                        try {
                            // Download and parse articles from this RSS source.
                            List<News> articles =
                                    rssService.loadNews(source);

                            // Skip this source if no article list is returned.
                            if (articles == null) {
                                continue;
                            }

                            // Process each article downloaded from the source.
                            for (News article : articles) {

                                // Ignore null entries.
                                if (article == null) {
                                    continue;
                                }

                                // Skip articles previously deleted by the user.
                                // NewsCleaner.key() creates the key used to
                                // identify an article in the deletion records.
                                if (deletedKeys.contains(
                                        NewsCleaner.key(article))) {
                                    continue;
                                }

                                // Add the article to the collection that will
                                // eventually be displayed in the GUI.
                                allNews.add(article);

                                // ------------------------------------
                                // SAVE ARTICLE TO THE DATABASE
                                // ------------------------------------

                                try {
                                    // Insert or update the article in the database.
                                    newsDAO.save(article);

                                } catch (Exception e) {

                                    // Report a database-saving error without
                                    // stopping the processing of other articles.
                                    publish("Could not save article: "
                                            + e.getMessage());
                                }
                            }

                        } catch (Exception e) {

                            // Report an error for this RSS source.
                            // The loop can continue with the next source.
                            publish("Could not load "
                                    + source.getName() + ": "
                                    + e.getMessage());
                        }
                    }

                } catch (Exception e) {

                    // Handle errors involving the database connection,
                    // deleted-article records, or RSS configuration.
                    publish("News loading failed: " + e.getMessage());
                }

                // Return all collected articles to the SwingWorker.
                // The done() method retrieves this result.
                return allNews;
            }

            // ----------------------------------------------------
            // PROCESS PROGRESS MESSAGES
            // ----------------------------------------------------

            // Runs on Swing's Event Dispatch Thread.
            // It receives messages previously sent with publish().
            protected void process(List<String> messages) {

                // Display each progress message in the GUI status bar.
                for (String message : messages) {

                    // Update the status text in the application window.
                    gui.setStatus(message);

                    // Also print the message to the console for debugging.
                    System.out.println(message);
                }
            }

            // ----------------------------------------------------
            // FINISH BACKGROUND PROCESSING
            // ----------------------------------------------------

            // Called automatically when doInBackground() finishes.
            // This method runs on Swing's Event Dispatch Thread.
            protected void done() {
                try {
                    // Retrieve the article collection returned by the
                    // background task. get() also reports task failures.
                    List<News> articles = get();

                    // Pass the downloaded articles to the GUI.
                    // NewsGUI updates its article list and applies filters.
                    gui.setNews(articles);

                    // Display the total number of collected articles.
                    gui.setStatus("Finished. Total news: "
                            + articles.size());

                } catch (Exception e) {

                    // Report an error if the background result could
                    // not be retrieved or the task failed.
                    gui.setStatus("Could not finish loading: "
                            + e.getMessage());

                } finally {

                    // Always re-enable the GUI controls when processing ends,
                    // whether the operation succeeds or fails.
                    gui.setLoading(false, null);
                }
            }
        };

        // Start the SwingWorker.
        // doInBackground() runs on a worker thread, keeping the GUI responsive.
        worker.execute();
    }
}
