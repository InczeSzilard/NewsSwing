package com.mycompany.swingaplication;

// Import Swing utilities for safely creating the GUI
// and performing background tasks.
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

// Used to save and read the feeds added with the Add URL button.
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

// Import collections used to store news articles and deleted article keys.
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main class of the news application.
 *
 * This class starts the graphical interface, connects the Refresh and
 * Add URL buttons to the news-loading process, and loads news from the
 * configured RSS sources (RSS.json plus the feeds added with Add URL).
 *
 * It also saves downloaded articles to the database and prevents articles
 * previously deleted by the user from being loaded again.
 */
public class SwingAplication {

    // File (in the program's working folder) that remembers the feeds the user
    // added with Add URL. One feed per line: name|url|language
    private static final String CUSTOM_FEEDS_FILE = "custom_feeds.txt";

    // Main method: the entry point of the Java application.
    public static void main(String[] args) {

        // Create and display Swing components on the Event Dispatch Thread.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // Create the main graphical user interface.
                final NewsGUI gui = new NewsGUI();

                // Connect the Refresh button to the RSS loading process.
                gui.setRefreshAction(new NewsGUI.RefreshHandler() {
                    public void refresh() {
                        loadNews(gui);
                    }
                });

                // Connect the Add URL button.
                // The feed is remembered in custom_feeds.txt and then all news
                // is loaded again, so the new feed is downloaded, saved to the
                // database and shown. Because it is remembered, it is also
                // included in every later Refresh and after restarting.
                gui.setAddUrlAction(new NewsGUI.AddUrlHandler() {
                    public void addUrl(String url, String language) {
                        try {
                            saveCustomSource(url, language);
                        } catch (IOException e) {
                            gui.setStatus("Could not save the feed: " + e.getMessage());
                            return;
                        }
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
    // FEEDS ADDED WITH THE "ADD URL" BUTTON
    // ------------------------------------------------------------

    // Reads the saved feeds from custom_feeds.txt (empty list if no file yet).
    private static List<RSSSource> loadCustomSources() {
        List<RSSSource> result = new ArrayList<RSSSource>();
        Path file = Paths.get(CUSTOM_FEEDS_FILE);

        if (!Files.exists(file)) {
            return result;
        }

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                // Each line is: name|url|language
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    result.add(new RSSSource(parts[0], parts[1], parts[2]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    // Adds a feed to custom_feeds.txt (does nothing if it is already saved).
    private static void saveCustomSource(String url, String language)
            throws IOException {

        // Already saved: nothing to do.
        for (RSSSource existing : loadCustomSources()) {
            if (existing.getUrl().equalsIgnoreCase(url)) {
                return;
            }
        }

        // Use the website name (e.g. "bbci.co.uk") as the source name.
        String name = url;
        try {
            name = new URL(url).getHost();
        } catch (Exception ignored) {
            // Keep the full URL as the name.
        }
        if (name.startsWith("www.")) {
            name = name.substring(4);
        }

        String line = name + "|" + url + "|" + language + System.lineSeparator();

        // CREATE: make the file if needed. APPEND: add to the end.
        Files.write(
                Paths.get(CUSTOM_FEEDS_FILE),
                line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    // ------------------------------------------------------------
    // LOAD NEWS FROM RSS SOURCES
    // ------------------------------------------------------------

    /**
     * Loads news articles from the configured RSS sources.
     *
     * A SwingWorker is used so that database operations and RSS downloads
     * run in the background without freezing the graphical interface.
     */
    private static void loadNews(final NewsGUI gui) {

        // Disable relevant controls and display a loading message.
        gui.setLoading(true, "Loading news...");

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
                    // by the user, so they do not reappear.
                    Set<String> deletedKeys =
                            cleaner.loadDeletedKeys();

                    // Create the RSS configuration loader.
                    RSSLoader loader = new RSSLoader();

                    // Read the configured RSS sources.
                    RSSparse sources = loader.loadSources();

                    // Stop if the configuration or source list is missing.
                    if (sources == null
                            || sources.getRssSources() == null) {
                        publish("No RSS sources found.");
                        return allNews;
                    }

                    // Combine the sources from RSS.json with the feeds added
                    // using Add URL. A feed that is already in RSS.json is
                    // not added a second time.
                    List<RSSSource> allSources = new ArrayList<RSSSource>();
                    for (RSSSource source : sources.getRssSources()) {
                        allSources.add(source);
                    }
                    for (RSSSource custom : loadCustomSources()) {
                        boolean duplicate = false;
                        for (RSSSource source : allSources) {
                            if (source.getUrl() != null
                                    && source.getUrl().equalsIgnoreCase(custom.getUrl())) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            allSources.add(custom);
                        }
                    }

                    // Create the service responsible for downloading
                    // and parsing RSS feeds.
                    RSSService rssService = new RSSService();

                    // Create the database access object used to save articles.
                    NewsDAO newsDAO = new NewsDAO();

                    // ------------------------------------------------
                    // PROCESS EACH RSS SOURCE
                    // ------------------------------------------------

                    for (RSSSource source : allSources) {

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
                                if (deletedKeys.contains(
                                        NewsCleaner.key(article))) {
                                    continue;
                                }

                                // Add the article to the collection that will
                                // eventually be displayed in the GUI.
                                allNews.add(article);

                                // Save the article to the database.
                                try {
                                    newsDAO.save(article);

                                } catch (Exception e) {
                                    // Report a saving error without stopping
                                    // the processing of other articles.
                                    publish("Could not save article: "
                                            + e.getMessage());
                                }
                            }

                        } catch (Exception e) {

                            // Report an error for this RSS source.
                            // The loop continues with the next source.
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

                // Return all collected articles; done() retrieves this result.
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