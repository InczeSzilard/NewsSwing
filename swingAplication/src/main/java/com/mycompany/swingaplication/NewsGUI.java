
package com.mycompany.swingaplication;

// Import Swing components used to create the graphical interface.
import javax.swing.*;

// Import events used to detect document changes and list selections.
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// Import AWT classes for layouts, dimensions, fonts, images, and desktop integration.
import java.awt.*;

// Import event classes for button clicks, focus changes, and mouse actions.
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Import classes used to open article URLs in the default web browser.
import java.net.URI;

// Import Normalizer to remove accents when searching article titles.
import java.text.Normalizer;

// Import collection classes used to store articles.
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * NewsGUI represents the graphical user interface of the news application.
 *
 * It allows users to:
 * - View saved news articles.
 * - Search articles by title.
 * - Filter articles by language.
 * - View article details.
 * - Open articles in a web browser.
 * - Add URLs through an external handler.
 * - Delete articles or clear the database.
 * - Refresh the news collection.
 *
 * The class extends JFrame, which represents the main application window.
 */
public class NewsGUI extends JFrame {

    // Stores the articles currently displayed in the JList.
    private final DefaultListModel newsModel =
            new DefaultListModel();

    // Displays the articles stored in newsModel.
    private final JList newsList =
            new JList(newsModel);

    // Allows users to enter text to search for article titles.
    private final JTextField searchField =
            new JTextField(20);

    // Popup menu used to display autocomplete suggestions.
    private final JPopupMenu suggestionsPopup =
            new JPopupMenu();

    // Maximum number of autocomplete suggestions displayed at once.
    private static final int MAX_SUGGESTIONS = 8;

    // Dropdown menu used to filter articles by language.
    private final JComboBox languageBox =
            new JComboBox(new String[] {
                "All",
                "English",
                "Romanian",
                "Hungarian"
            });

    // Button used to add a news article or RSS feed URL.
    private final JButton addButton =
            new JButton("Add URL");

    // Button used to delete the selected article.
    private final JButton deleteButton =
            new JButton("Delete Selected");

    // Button used to open the selected article in a browser.
    private final JButton openButton =
            new JButton("Open in Browser");

    // Button used to download or reload news.
    private final JButton refreshButton =
            new JButton("Refresh");

    // Button used to remove all saved news and deletion records.
    private final JButton clearDatabaseButton =
            new JButton("Clear Database");

    // Displays the title, source, description, and URL of an article.
    private final JTextArea newsContent =
            new JTextArea();

    // Displays messages about the current application state.
    private final JLabel statusLabel =
            new JLabel("Ready");

    // JLabel used to display the cover image at the top of the window.
    private JLabel coverLabel;

    // Stores the scaled cover image.
    private ImageIcon coverIcon;

    // Stores all articles received from the application controller.
    // Filtering is performed on this collection.
    private List<News> allNews =
            new ArrayList<News>();

    // Handles operations involving article deletion and database clearing.
    private final NewsCleaner newsCleaner =
            new NewsCleaner();

    /**
     * Interface for connecting the Refresh button to the class
     * responsible for downloading or reloading news.
     */
    public interface RefreshHandler {
        void refresh();
    }

    /**
     * Interface for connecting the Add URL button to the class
     * responsible for downloading an article or RSS feed.
     */
    public interface AddUrlHandler {
        void addUrl(String url);
    }

    // References to the external operations supplied by the controller.
    private RefreshHandler refreshAction;
    private AddUrlHandler addUrlAction;

    /**
     * Constructor of the NewsGUI window.
     * Initializes the interface, registers event handlers,
     * and configures the window dimensions and position.
     */
    public NewsGUI() {
        super("News Application");

        // Create and arrange all visual components.
        buildGUI();

        // Connect buttons, search fields, and selection events.
        addEvents();

        // Close the application when the window is closed.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set the initial window size.
        setSize(1200, 750);

        // Prevent the window from becoming too small.
        setMinimumSize(new Dimension(850, 500));

        // Center the window on the screen.
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------
    // LOAD THE COVER IMAGE
    // ------------------------------------------------------------

    /**
     * Loads the cover image from a specified file path.
     *
     * If the image exists, it is scaled while preserving its
     * aspect ratio. If the image cannot be found, a text-based
     * WORLD NEWS banner is displayed instead.
     */
    private void createCoverImage() {

        // Absolute path to the cover image on the local computer.
        String imagePath =
            "C:\\Users\\szila\\Documents\\NetBeansProjects\\swingAplication\\src\\main\\java\\com\\mycompany\\swingaplication\\coverimage.jpg";

        // Create a File object representing the image file.
        java.io.File imageFile = new java.io.File(imagePath);

        // Check that the path points to an existing regular file.
        if (imageFile.exists() && imageFile.isFile()) {

            // Load the original image.
            ImageIcon originalIcon = new ImageIcon(imagePath);

            Image originalImage = originalIcon.getImage();

            // Obtain the original image dimensions.
            int originalWidth = originalIcon.getIconWidth();
            int originalHeight = originalIcon.getIconHeight();

            // Define the target dimensions for the cover area.
            int targetWidth = 7000;
            int targetHeight = 400;

            // Calculate a scaling factor that fits the image
            // inside the target dimensions without distortion.
            double scale = Math.min(
                (double) targetWidth / originalWidth,
                (double) targetHeight / originalHeight
            );

            // Calculate the scaled image dimensions.
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

            // Resize the image using smooth interpolation.
            Image scaledImage = originalImage.getScaledInstance(
                scaledWidth,
                scaledHeight,
                Image.SCALE_SMOOTH
            );

            // Store the scaled image in an ImageIcon.
            coverIcon = new ImageIcon(scaledImage);

            // Create a label that displays the cover image.
            coverLabel = new JLabel(
                coverIcon,
                SwingConstants.CENTER
            );

            // Set the preferred size of the cover label.
            coverLabel.setPreferredSize(
                new Dimension(targetWidth, targetHeight)
            );

            // Print a message to the console for debugging.
            System.out.println("Cover image loaded: " + imagePath);

        } else {

            // If the image is missing, create a text banner.
            coverLabel = new JLabel(
                "WORLD NEWS",
                SwingConstants.CENTER
            );

            // Set the banner text font.
            coverLabel.setFont(
                new Font("Arial", Font.BOLD, 30)
            );

            // Set white text on a dark blue background.
            coverLabel.setForeground(Color.WHITE);
            coverLabel.setBackground(new Color(20, 40, 75));

            // Make the background color visible.
            coverLabel.setOpaque(true);

            // Print an error message to the console.
            System.err.println("Image not found: " + imagePath);
        }
    }

    // ------------------------------------------------------------
    // BUILD THE GUI
    // ------------------------------------------------------------

    /**
     * Creates and arranges the graphical components.
     *
     * The main window contains:
     * - A header with the cover image and search controls.
     * - A split area containing the article list and article details.
     * - A status bar at the bottom.
     */
    private void buildGUI() {

        // Use BorderLayout to divide the window into five regions.
        setLayout(new BorderLayout(5, 5));

        // Load the cover image or the fallback banner.
        createCoverImage();

        // Create a panel for the cover image.
        JPanel coverPanel =
                new JPanel(new BorderLayout());

        // Place the cover image in the center of its panel.
        coverPanel.add(
                coverLabel,
                BorderLayout.CENTER
        );

        // Create a panel containing the search controls and buttons.
        JPanel topPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Add the language filter.
        topPanel.add(new JLabel("Language:"));
        topPanel.add(languageBox);

        // Add the article title search field.
        topPanel.add(new JLabel("Search title:"));
        topPanel.add(searchField);

        // Add the application action buttons.
        topPanel.add(addButton);
        topPanel.add(deleteButton);
        topPanel.add(openButton);
        topPanel.add(refreshButton);
        topPanel.add(clearDatabaseButton);

        // Explain how to open an article using the tooltip.
        openButton.setToolTipText(
                "Open the selected article or double-click it"
        );

        // Allow only one article to be selected at a time.
        newsList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        // Set the font used for article titles in the list.
        newsList.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );

        // Prevent users from editing the article details.
        newsContent.setEditable(false);

        // Enable automatic line wrapping in the article details.
        newsContent.setLineWrap(true);
        newsContent.setWrapStyleWord(true);

        // Set the font used to display article details.
        newsContent.setFont(
                new Font("Arial", Font.PLAIN, 15)
        );

        // Add scrollbars to the article list.
        JScrollPane leftPanel =
                new JScrollPane(newsList);

        // Add scrollbars to the article details area.
        JScrollPane rightPanel =
                new JScrollPane(newsContent);

        // Divide the main content area into two resizable sections.
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel
        );

        // Set the initial width of the article list.
        splitPane.setDividerLocation(350);

        // Allocate approximately 35% of the extra width to the left.
        splitPane.setResizeWeight(0.35);

        // Add padding around the status message.
        statusLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        5, 10, 5, 10
                )
        );

        // Create the header containing the cover and controls.
        JPanel headerPanel =
                new JPanel(new BorderLayout());

        // Place the cover image at the top of the header.
        headerPanel.add(
                coverPanel,
                BorderLayout.CENTER
        );

        // Place the search controls below the cover image.
        headerPanel.add(
                topPanel,
                BorderLayout.SOUTH
        );

        // Add the header to the top of the main window.
        add(headerPanel, BorderLayout.NORTH);

        // Add the article list and details to the center.
        add(splitPane, BorderLayout.CENTER);

        // Add the status bar to the bottom.
        add(statusLabel, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------
    // EVENT HANDLERS
    // ------------------------------------------------------------

    /**
     * Registers listeners for button clicks, search changes,
     * language changes, article selection, and focus changes.
     */
    private void addEvents() {

        // ADD URL:
        // Ask the user for an article URL or RSS feed URL.
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // Display a dialog containing a text input field.
                String url = JOptionPane.showInputDialog(
                        NewsGUI.this,
                        "Enter a news article or RSS feed URL:",
                        "Add News",
                        JOptionPane.PLAIN_MESSAGE
                );

                // Stop if the user cancels or enters an empty value.
                if (url == null || url.trim().length() == 0) {
                    return;
                }

                // Remove leading and trailing whitespace.
                final String cleanUrl = url.trim();

                // Perform a basic check for an HTTP or HTTPS URL.
                if (!cleanUrl.matches("(?i)^https?://\\S+$")) {

                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Please enter a valid HTTP or HTTPS URL.",
                            "Invalid URL",
                            JOptionPane.WARNING_MESSAGE
                    );

                    return;
                }

                // Pass the URL to the configured external handler.
                if (addUrlAction != null) {
                    addUrlAction.addUrl(cleanUrl);
                } else {

                    // Display a message if no handler was configured.
                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "The URL downloader has not been connected.",
                            "Add News",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });

        // DELETE SELECTED ARTICLE:
        // Delete the currently selected article from storage.
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // Get the selected News object from the list.
                News selected =
                        (News) newsList.getSelectedValue();

                // Stop if no article is selected.
                if (selected == null) {
                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Please select an article to delete.",
                            "No Article Selected",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }

                // Ask the user to confirm permanent deletion.
                int answer = JOptionPane.showConfirmDialog(
                        NewsGUI.this,
                        "Permanently delete this article?\n\n"
                                + selected.getTitle(),
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                // Continue only if the user confirms.
                if (answer == JOptionPane.YES_OPTION) {
                    try {

                        // Delete the article using the database helper.
                        newsCleaner.deleteArticle(selected);

                        // Remove the article from the in-memory collection.
                        allNews.remove(selected);

                        // Close and clear the autocomplete suggestions.
                        suggestionsPopup.setVisible(false);

                        // Rebuild the list using the current filters.
                        applyFilters();

                        // Update the status bar.
                        setStatus("Article permanently deleted.");

                    } catch (Exception ex) {

                        // Print details for debugging.
                        ex.printStackTrace();

                        // Inform the user that deletion failed.
                        JOptionPane.showMessageDialog(
                                NewsGUI.this,
                                "Could not delete the article:\n"
                                        + ex.getMessage(),
                                "Database Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });

        // OPEN ARTICLE:
        // Open the selected article in the default browser.
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openSelectedUrl();
            }
        });

        // DOUBLE-CLICK ARTICLE:
        // Open the article when the user double-clicks a list entry.
        newsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedUrl();
                }
            }
        });

        // REFRESH NEWS:
        // Ask the external controller to download or reload articles.
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // Hide autocomplete while refreshing.
                suggestionsPopup.setVisible(false);

                // Execute the refresh operation if it is configured.
                if (refreshAction != null) {
                    refreshAction.refresh();
                } else {

                    // Inform the user if no refresh handler exists.
                    setStatus(
                            "Refresh action has not been configured."
                    );
                }
            }
        });

        // CLEAR DATABASE:
        // Delete all saved news and deletion records.
        clearDatabaseButton.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // Warn the user before deleting all stored records.
                int answer = JOptionPane.showConfirmDialog(
                        NewsGUI.this,
                        "This will delete ALL saved news and "
                                + "deletion records.\n\n"
                                + "Click Refresh afterwards to download "
                                + "news again.\n\nContinue?",
                        "Clear Database",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                // Do nothing unless the user confirms.
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }

                try {

                    // Remove stored records through the helper class.
                    newsCleaner.clearAll();

                    // Clear the in-memory article collection.
                    allNews.clear();

                    // Clear the visible article list and details.
                    newsModel.clear();
                    newsContent.setText("");

                    // Hide autocomplete suggestions.
                    suggestionsPopup.setVisible(false);

                    // Inform the user that the operation has completed.
                    setStatus(
                            "Database cleared. Click Refresh to reload news."
                    );

                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Database cleared successfully.\n"
                                    + "Click Refresh to download news again.",
                            "Database Cleared",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (Exception ex) {

                    // Log the error for debugging.
                    ex.printStackTrace();

                    // Inform the user that clearing failed.
                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Could not clear the database:\n"
                                    + ex.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // SEARCH FIELD:
        // Listen for changes to the text entered by the user.
        searchField.getDocument().addDocumentListener(
                new DocumentListener() {

            // Called when text is inserted.
            public void insertUpdate(DocumentEvent e) {
                updateSearch();
            }

            // Called when text is removed.
            public void removeUpdate(DocumentEvent e) {
                updateSearch();
            }

            // Called when document attributes change.
            public void changedUpdate(DocumentEvent e) {
                updateSearch();
            }
        });

        // LANGUAGE FILTER:
        // Reapply filters whenever the selected language changes.
        languageBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // Close autocomplete suggestions.
                suggestionsPopup.setVisible(false);

                // Update the visible article list.
                applyFilters();
            }
        });

        // ARTICLE SELECTION:
        // Display the details of the selected article.
        newsList.addListSelectionListener(
                new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {

                // Ignore intermediate events while selection is changing.
                if (!e.getValueIsAdjusting()) {
                    showSelectedNews();
                }
            }
        });

        // HIDE SUGGESTIONS WHEN FOCUS IS LOST:
        // Close autocomplete if the search field loses focus and
        // the popup does not own focus.
        searchField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {

                // Run after the current focus event has been processed.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        if (!searchField.hasFocus()
                                && !suggestionsPopup.isFocusOwner()) {

                            // Hide the suggestions popup.
                            suggestionsPopup.setVisible(false);
                        }
                    }
                });
            }
        });
    }

    // ------------------------------------------------------------
    // OPEN ARTICLE IN BROWSER
    // ------------------------------------------------------------

    /**
     * Opens the URL of the selected article in the system's
     * default browser.
     *
     * Uses Desktop.browse() when supported and provides a
     * Windows-specific fallback.
     */
    private void openSelectedUrl() {

        // Get the currently selected article.
        News news =
                (News) newsList.getSelectedValue();

        // Stop if there is no selected article.
        if (news == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an article to open.",
                    "No Article Selected",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Obtain the article URL.
        String url = news.getUrl();

        // Check that the URL is not null or empty.
        if (url == null || url.trim().length() == 0) {
            setStatus("This article has no URL.");
            return;
        }

        // Remove leading and trailing whitespace.
        url = url.trim();

        // Accept only strings beginning with HTTP or HTTPS.
        if (!url.matches("(?i)^https?://\\S+$")) {
            setStatus("Cannot open this URL: " + url);
            return;
        }

        try {

            // Prefer the standard Java Desktop browsing API.
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(
                            Desktop.Action.BROWSE)) {

                // Convert the URL string to a URI and open it.
                Desktop.getDesktop().browse(new URI(url));

                setStatus("Opened in browser: " + url);
                return;
            }

            // Identify the operating system if Desktop browsing
            // is not available.
            String os = System.getProperty("os.name");

            // Use the Windows URL handler as a fallback.
            if (os != null
                    && os.toLowerCase(Locale.ENGLISH)
                            .indexOf("windows") >= 0) {

                Runtime.getRuntime().exec(new String[] {
                    "rundll32",
                    "url.dll,FileProtocolHandler",
                    url
                });

                setStatus("Opened in browser: " + url);

            } else {

                // Display a message when browser opening is unsupported.
                setStatus(
                        "Browser opening is not supported on this system."
                );
            }

        } catch (Exception ex) {

            // Log the exception and update the status bar.
            ex.printStackTrace();
            setStatus("Could not open URL: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // METHODS USED BY SwingAplication
    // ------------------------------------------------------------

    /**
     * Registers the external operation used by the Refresh button.
     *
     * The main application/controller supplies an implementation
     * of RefreshHandler.
     */
    public void setRefreshAction(RefreshHandler action) {
        refreshAction = action;
    }

    /**
     * Registers the external operation used by the Add URL button.
     *
     * The main application/controller supplies an implementation
     * that processes the entered URL.
     */
    public void setAddUrlAction(AddUrlHandler action) {
        addUrlAction = action;
    }

    /**
     * Receives the complete collection of news articles and refreshes
     * the visible list according to the current search and language.
     *
     * Swing components should be updated on the Event Dispatch Thread.
     */
    public void setNews(List<News> news) {

        // If this method is called from a background thread,
        // schedule the update on Swing's Event Dispatch Thread.
        if (!SwingUtilities.isEventDispatchThread()) {

            // Copy the collection to avoid depending on the original
            // list while the UI update is waiting to execute.
            final List<News> copy =
                    news == null
                    ? new ArrayList<News>()
                    : new ArrayList<News>(news);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setNews(copy);
                }
            });

            return;
        }

        // Replace the collection used for filtering and suggestions.
        allNews = news == null
                ? new ArrayList<News>()
                : new ArrayList<News>(news);

        // Refresh the visible article list.
        applyFilters();
    }

    /**
     * Enables or disables the main controls while an operation
     * such as downloading news is in progress.
     *
     * The message parameter optionally updates the status bar.
     */
    public void setLoading(
            final boolean loading, final String message) {

        // Ensure component updates happen on the Event Dispatch Thread.
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setLoading(loading, message);
                }
            });
            return;
        }

        // Hide suggestions while the application is busy.
        if (loading) {
            suggestionsPopup.setVisible(false);
        }

        // Disable the controls when loading is true.
        // Re-enable them when loading is false.
        searchField.setEnabled(!loading);
        addButton.setEnabled(!loading);
        deleteButton.setEnabled(!loading);
        openButton.setEnabled(!loading);
        refreshButton.setEnabled(!loading);
        clearDatabaseButton.setEnabled(!loading);
        languageBox.setEnabled(!loading);

        // Update the status message if one was provided.
        if (message != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * Updates the status bar with a message.
     *
     * If necessary, schedules the update on Swing's Event Dispatch
     * Thread to keep Swing component access thread-safe.
     */
    public void setStatus(final String message) {

        // Move the update to the Event Dispatch Thread if necessary.
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setStatus(message);
                }
            });
            return;
        }

        // Use a blank string if the supplied message is null.
        statusLabel.setText(message == null ? " " : message);
    }

    // ------------------------------------------------------------
    // SEARCH AND AUTOCOMPLETE
    // ------------------------------------------------------------

    /**
     * Normalizes text before comparison.
     *
     * This method:
     * 1. Handles null values.
     * 2. Separates accented characters from their accent marks.
     * 3. Removes accent marks.
     * 4. Converts text to lowercase.
     * 5. Removes leading and trailing whitespace.
     *
     * For example, "Știri" becomes "stiri".
     */
    private String normalizeText(String text) {

        // Return an empty string if the input is null.
        if (text == null) {
            return "";
        }

        return Normalizer.normalize(
                    text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /**
     * Determines whether an article matches the selected language.
     *
     * If "All" is selected, every language is accepted.
     */
    private boolean languageMatches(News news) {

        // Read the currently selected language.
        String selectedLanguage =
                (String) languageBox.getSelectedItem();

        // Accept all articles if no language is selected
        // or the "All" option is selected.
        if (selectedLanguage == null
                || selectedLanguage.equals("All")) {
            return true;
        }

        // Compare the article language with the selected language.
        return news.getLanguage() != null
                && news.getLanguage().trim()
                        .equalsIgnoreCase(selectedLanguage);
    }

    /**
     * Updates the article list and autocomplete suggestions
     * whenever the search field changes.
     */
    private void updateSearch() {

        // Apply the current search text and language filter.
        applyFilters();

        // Display matching autocomplete suggestions.
        showTitleSuggestions();
    }

    /**
     * Displays a popup menu containing article titles that
     * match the user's current search query.
     *
     * Suggestions respect the selected language and are limited
     * to MAX_SUGGESTIONS.
     */
    private void showTitleSuggestions() {

        // Close previous suggestions and remove their menu items.
        suggestionsPopup.setVisible(false);
        suggestionsPopup.removeAll();

        // Normalize the search text for accent-insensitive matching.
        final String query =
                normalizeText(searchField.getText());

        // Do not display suggestions for an empty query or
        // when the search field is disabled.
        if (query.length() == 0 || !searchField.isEnabled()) {
            return;
        }

        // Count the number of suggestions created.
        int count = 0;

        // Examine each article in the complete news collection.
        for (News news : allNews) {

            // Skip articles without a title.
            if (news == null || news.getTitle() == null) {
                continue;
            }

            // Exclude articles that do not match the language filter.
            if (!languageMatches(news)) {
                continue;
            }

            // Obtain the title and remove surrounding whitespace.
            final String title = news.getTitle().trim();

            // Check whether the normalized title contains the query.
            if (normalizeText(title).contains(query)) {

                // Create a clickable suggestion.
                JMenuItem suggestion =
                        new JMenuItem(title);

                // Fill the search field when the suggestion is clicked.
                suggestion.addActionListener(
                        new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        // Close the popup menu.
                        suggestionsPopup.setVisible(false);

                        // Replace the query with the selected title.
                        searchField.setText(title);

                        // Return keyboard focus to the search field.
                        searchField.requestFocusInWindow();

                        // Refresh the visible articles.
                        applyFilters();
                    }
                });

                // Add the suggestion to the popup menu.
                suggestionsPopup.add(suggestion);
                count++;

                // Stop after reaching the maximum number of suggestions.
                if (count >= MAX_SUGGESTIONS) {
                    break;
                }
            }
        }

        // Show the popup directly below the search field if
        // at least one suggestion exists and the field is visible.
        if (count > 0 && searchField.isShowing()) {
            suggestionsPopup.show(
                    searchField, 0, searchField.getHeight()
            );
        }
    }

    // ------------------------------------------------------------
    // FILTER AND DISPLAY ARTICLES
    // ------------------------------------------------------------

    /**
     * Filters the complete article collection using:
     * - The text entered in the search field.
     * - The language selected in the dropdown.
     *
     * It updates the JList model and attempts to preserve the
     * currently selected article.
     */
    private void applyFilters() {

        // Normalize the search query.
        String query =
                normalizeText(searchField.getText());

        // Read the selected language for the status message.
        String selectedLanguage =
                (String) languageBox.getSelectedItem();

        // Remember the selected article before rebuilding the list.
        News previouslySelected =
                (News) newsList.getSelectedValue();

        // Remove all existing visible entries.
        newsModel.clear();

        // Examine every article in the complete collection.
        for (News news : allNews) {

            // Skip null entries.
            if (news == null) {
                continue;
            }

            // Normalize the title for accent-insensitive matching.
            String title =
                    normalizeText(news.getTitle());

            // Add the article only if both filters match.
            if (languageMatches(news)
                    && title.contains(query)) {
                newsModel.addElement(news);
            }
        }

        // Preserve the old selection if the article is still visible.
        if (previouslySelected != null
                && newsModel.contains(previouslySelected)) {

            newsList.setSelectedValue(previouslySelected, true);

        } else if (!newsModel.isEmpty()) {

            // Otherwise select the first visible article.
            newsList.setSelectedIndex(0);

        } else {

            // Clear the details panel if no articles match.
            newsContent.setText("");
        }

        // Prepare a status message showing the number of results.
        String message = "Showing "
                + newsModel.size() + " article(s)";

        // Include the selected language when it is not "All".
        if (selectedLanguage != null
                && !selectedLanguage.equals("All")) {
            message += " in " + selectedLanguage;
        }

        // Display the final status message.
        statusLabel.setText(message + ".");
    }

    /**
     * Displays the details of the currently selected article
     * in the newsContent text area.
     */
    private void showSelectedNews() {

        // Retrieve the selected article.
        News news =
                (News) newsList.getSelectedValue();

        // Clear the details area if there is no selection.
        if (news == null) {
            newsContent.setText("");
            return;
        }

        // Display the article information using section headings.
        newsContent.setText(
                "TITLE\n"
                + "========================================\n\n"
                + safeText(news.getTitle()) + "\n\n"

                + "SOURCE\n"
                + "========================================\n\n"
                + safeText(news.getSource()) + "\n\n"

                + "DESCRIPTION\n"
                + "========================================\n\n"
                + safeText(news.getDescription()) + "\n\n"

                + "URL\n"
                + "========================================\n\n"
                + safeText(news.getUrl())
        );

        // Move the caret to the beginning of the text area.
        newsContent.setCaretPosition(0);
    }

    /**
     * Returns the provided text if it is available.
     *
     * If the text is null or blank, returns "(Not available)"
     * to avoid displaying empty article fields.
     */
    private String safeText(String text) {

        return text == null || text.trim().length() == 0
                ? "(Not available)"
                : text;
    }
}
