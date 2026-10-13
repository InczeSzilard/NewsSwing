package com.mycompany.swingaplication;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Main Swing interface for the news application.
 *
 * Keeps the existing search, autocomplete, language filter, article details,
 * add URL, delete, clear database, refresh, and browser-opening features.
 * Includes an indeterminate progress bar while the controller updates news.
 *
 * How the pieces fit together:
 *  - allNews    : the full list of articles (everything loaded from the database).
 *  - newsModel  : only the articles currently shown (allNews after filtering).
 *  - applyFilters() rebuilds newsModel from allNews using the language box
 *    and the search field.
 */
public class NewsGUI extends JFrame {

    // ------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------

    // Model behind the article list: holds only the articles that pass the filters.
    private final DefaultListModel<News> newsModel = new DefaultListModel<>();

    // The visible list on the left side; displays News.toString() (the title).
    private final JList<News> newsList = new JList<>(newsModel);

    // Text box used to search article titles.
    private final JTextField searchField = new JTextField(20);

    // Dropdown that shows title suggestions under the search field.
    private final JPopupMenu suggestionsPopup = new JPopupMenu();

    // Maximum number of suggestions shown in the popup.
    private static final int MAX_SUGGESTIONS = 8;

    // Language filter. The text must match the cases in languageMatches().
    private final JComboBox<String> languageBox = new JComboBox<>(
            new String[]{"All", "English", "Romanian", "Hungarian"});

    // Toolbar buttons.
    private final JButton addButton = new JButton("Add URL");
    private final JButton deleteButton = new JButton("Delete Selected");
    private final JButton openButton = new JButton("Open in Browser");
    private final JButton toggleReadButton = new JButton("Mark Read/Unread");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton updateButton = new JButton("Update News");
    private final JButton clearDatabaseButton = new JButton("Clear Database");

    // Right-hand panel showing the details of the selected article (read-only).
    private final JTextArea newsContent = new JTextArea();

    // Status line at the bottom of the window.
    private final JLabel statusLabel = new JLabel("Ready");

    // Busy indicators, visible only while the news is being updated.
    private final JLabel loadingIconLabel = new JLabel("⟳");
    private final JProgressBar loadingBar = new JProgressBar();

    // Banner at the top: the cover image, or a fallback text banner.
    private JLabel coverLabel;
    private ImageIcon coverIcon;

    // Every article currently loaded (unfiltered).
    private List<News> allNews = new ArrayList<>();

    // Helper class that deletes articles / clears the database.
    private final NewsCleaner newsCleaner = new NewsCleaner();

    // File (in the program's working folder) that remembers which articles were read.
    private static final String READ_FILE = "read_news.txt";

    // Keys ("source|url") of the articles the user has read.
    private final Set<String> readKeys = new HashSet<>();

    // True while an update is running; prevents starting a second one.
    private boolean loading = false;

    // True once a refresh action has been connected (currently not read anywhere).
    private boolean updateHandlerConfigured = false;

    // ------------------------------------------------------------
    // CALLBACK INTERFACES
    // The controller implements these so the GUI can ask it to do work
    // without knowing how the work is done.
    // ------------------------------------------------------------

    // Called when the user clicks Refresh or Update News.
    public interface RefreshHandler {
        void refresh();
    }

    // Called when the user adds a URL; receives the validated URL and the
    // language chosen for it ("English", "Romanian" or "Hungarian").
    public interface AddUrlHandler {
        void addUrl(String url, String language);
    }

    private RefreshHandler refreshAction;
    private AddUrlHandler addUrlAction;

    // ------------------------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------------------------

    public NewsGUI() {
        super("News Application");   // window title
        loadReadState();             // restore which articles were read last time
        buildGUI();                  // create and arrange the components
        addEvents();                 // attach listeners to the components

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // closing the window ends the program
        setSize(1200, 750);
        setMinimumSize(new Dimension(850, 500));
        setLocationRelativeTo(null); // center on screen
    }

    // ------------------------------------------------------------
    // COVER IMAGE
    // ------------------------------------------------------------

    // Loads the banner image from the project resources and scales it to fit;
    // falls back to a plain text banner if the image cannot be found or read.
    private void createCoverImage() {
        // The image is loaded from the classpath (src/main/resources), not from
        // a path on this computer. It is packaged inside the .jar, so it works
        // for every user. The leading "/" means "start from the resources root".
        java.net.URL imageUrl = NewsGUI.class.getResource(
                "/com/mycompany/swingaplication/coverimage.jpg");

        // getResource returns null when the file is not in the resources.
        if (imageUrl == null) {
            createFallbackCover();
            System.err.println("Cover image not found in the resources.");
            return;
        }

        ImageIcon originalIcon = new ImageIcon(imageUrl);
        Image originalImage = originalIcon.getImage();

        int originalWidth = originalIcon.getIconWidth();
        int originalHeight = originalIcon.getIconHeight();

        // Size of the banner area.
        int targetWidth = 1100;
        int targetHeight = 300;

        // Width/height are <= 0 when the image could not be decoded.
        if (originalWidth > 0 && originalHeight > 0) {
            // Pick the smaller ratio so the image fits inside the target
            // box while keeping its proportions.
            double scale = Math.min(
                    (double) targetWidth / originalWidth,
                    (double) targetHeight / originalHeight);

            // Math.max(1, ...) guarantees the size is never 0.
            int scaledWidth = Math.max(1, (int) (originalWidth * scale));
            int scaledHeight = Math.max(1, (int) (originalHeight * scale));

            Image scaledImage = originalImage.getScaledInstance(
                    scaledWidth, scaledHeight, Image.SCALE_SMOOTH);

            coverIcon = new ImageIcon(scaledImage);
            coverLabel = new JLabel(coverIcon, SwingConstants.CENTER);
            coverLabel.setPreferredSize(new Dimension(targetWidth, targetHeight));
            System.out.println("Cover image loaded: " + imageUrl);
        } else {
            createFallbackCover();
        }
    }

    // Plain dark-blue banner with white text, used when the image is unavailable.
    private void createFallbackCover() {
        coverLabel = new JLabel("WORLD NEWS", SwingConstants.CENTER);
        coverLabel.setFont(new Font("Arial", Font.BOLD, 30));
        coverLabel.setForeground(Color.WHITE);
        coverLabel.setBackground(new Color(20, 40, 75));
        coverLabel.setOpaque(true);  // without this the background color is not painted
        coverLabel.setPreferredSize(new Dimension(1100, 100));
    }

    // ------------------------------------------------------------
    // BUILD GUI
    // ------------------------------------------------------------

    // Creates the layout:
    //   NORTH  : header = cover banner (center) + toolbar (south)
    //   CENTER : split pane = article list | article details
    //   SOUTH  : loading indicator (west) + status text (center)
    private void buildGUI() {
        setLayout(new BorderLayout(5, 5));
        createCoverImage();

        JPanel coverPanel = new JPanel(new BorderLayout());
        coverPanel.add(coverLabel, BorderLayout.CENTER);

        // Toolbar in two rows, so no button is cut off when the window is narrow.
        // Row 1: language filter and search box.
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterRow.add(new JLabel("Language:"));
        filterRow.add(languageBox);
        filterRow.add(new JLabel("Search title:"));
        filterRow.add(searchField);

        // Row 2: all the buttons (Clear Database is the last one).
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonRow.add(addButton);
        buttonRow.add(deleteButton);
        buttonRow.add(openButton);
        buttonRow.add(toggleReadButton);
        buttonRow.add(refreshButton);
        buttonRow.add(updateButton);
        buttonRow.add(clearDatabaseButton);

        // Stack the two rows on top of each other.
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(filterRow);
        topPanel.add(buttonRow);

        // Tooltips shown when hovering over the buttons.
        openButton.setToolTipText("Open the selected article or double-click it");
        toggleReadButton.setToolTipText("Mark the selected article as read or unread");
        refreshButton.setToolTipText("Run the configured refresh action");
        updateButton.setToolTipText("Download/reload the latest news");

        // Only one article can be selected at a time.
        newsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        newsList.setFont(new Font("Arial", Font.PLAIN, 14));

        // Custom row painting: unread = bold with a "●" marker,
        // read = normal weight in gray.
        newsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                    Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // Let the default renderer set up colors, selection, etc.
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof News) {
                    News n = (News) value;
                    if (isRead(n)) {
                        setText("    " + n.getTitle());
                        setFont(list.getFont().deriveFont(Font.PLAIN));
                        if (!isSelected) {
                            setForeground(Color.GRAY);
                        }
                    } else {
                        setText("\u25CF " + n.getTitle());
                        setFont(list.getFont().deriveFont(Font.BOLD));
                    }
                }
                return this;
            }
        });

        // Details area: read-only, wraps long lines at word boundaries.
        newsContent.setEditable(false);
        newsContent.setLineWrap(true);
        newsContent.setWrapStyleWord(true);
        newsContent.setFont(new Font("Arial", Font.PLAIN, 15));

        // Scroll bars for both sides.
        JScrollPane leftPanel = new JScrollPane(newsList);
        JScrollPane rightPanel = new JScrollPane(newsContent);

        // Resizable divider between the list and the details.
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.35);  // the list gets 35% of any extra width

        // Animated busy area: indeterminate bar plus a spinner-like symbol.
        JPanel loadingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        loadingIconLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        loadingIconLabel.setForeground(new Color(30, 100, 190));
        loadingBar.setIndeterminate(true);   // endless animation, no percentage
        loadingBar.setStringPainted(true);   // show text inside the bar
        loadingBar.setString("Updating news...");
        loadingBar.setPreferredSize(new Dimension(190, 20));
        // Hidden until setLoading(true, ...) is called.
        loadingBar.setVisible(false);
        loadingIconLabel.setVisible(false);
        loadingPanel.add(loadingIconLabel);
        loadingPanel.add(loadingBar);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(loadingPanel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(coverPanel, BorderLayout.CENTER);
        headerPanel.add(topPanel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------
    // EVENTS
    // ------------------------------------------------------------

    // Connects every component to its behavior.
    private void addEvents() {

        // ----- Add URL -----
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ask the user for a URL. Returns null if they press Cancel.
                String url = JOptionPane.showInputDialog(
                        NewsGUI.this,
                        "Enter a news article or RSS feed URL:",
                        "Add News",
                        JOptionPane.PLAIN_MESSAGE);

                // Cancelled or empty: do nothing.
                if (url == null || url.trim().isEmpty()) {
                    return;
                }

                // Accept only http:// or https:// followed by non-space characters.
                final String cleanUrl = url.trim();
                if (!cleanUrl.matches("(?i)^https?://\\S+$")) {
                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Please enter a valid HTTP or HTTPS URL.",
                            "Invalid URL",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Ask which language this feed is in, so the language filter
                // works for it. Returns null if the user presses Cancel.
                String[] languages = {"English", "Romanian", "Hungarian"};
                String language = (String) JOptionPane.showInputDialog(
                        NewsGUI.this,
                        "Language of this feed:",
                        "Add News",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        languages,
                        languages[0]);

                if (language == null) {
                    return;
                }

                // Hand the URL and language to the controller, if connected.
                if (addUrlAction != null) {
                    addUrlAction.addUrl(cleanUrl, language);
                } else {
                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "The URL downloader has not been connected.",
                            "Add News",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        // ----- Delete Selected -----
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                News selected = newsList.getSelectedValue();

                // Nothing selected: tell the user and stop.
                if (selected == null) {
                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Please select an article to delete.",
                            "No Article Selected",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // Ask for confirmation, showing the article title.
                int answer = JOptionPane.showConfirmDialog(
                        NewsGUI.this,
                        "Permanently delete this article?\n\n" + selected.getTitle(),
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (answer == JOptionPane.YES_OPTION) {
                    try {
                        newsCleaner.deleteArticle(selected); // remove from the database
                        allNews.remove(selected);            // remove from the in-memory list
                        readKeys.remove(readKey(selected));  // forget its read status
                        saveReadState();
                        suggestionsPopup.setVisible(false);
                        applyFilters();                      // refresh the visible list
                        setStatus("Article permanently deleted.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(
                                NewsGUI.this,
                                "Could not delete the article:\n" + ex.getMessage(),
                                "Database Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // ----- Open in Browser (button) -----
        openButton.addActionListener(e -> openSelectedUrl());

        // ----- Open in Browser (double-click on an article) -----
        newsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Find the clicked row; ignore clicks on empty space below the rows.
                int index = newsList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    Rectangle bounds = newsList.getCellBounds(index, index);
                    if (bounds != null && bounds.contains(e.getPoint())) {
                        markRead(newsModel.get(index), true);  // clicking = reading
                    }
                }

                if (e.getClickCount() == 2) {
                    openSelectedUrl();
                }
            }
        });

        // ----- Mark Read/Unread: toggles the status of the selected article -----
        toggleReadButton.addActionListener(e -> {
            News selected = newsList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(
                        NewsGUI.this,
                        "Please select an article first.",
                        "No Article Selected",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            boolean nowRead = !isRead(selected);
            markRead(selected, nowRead);
            setStatus(nowRead ? "Marked as read." : "Marked as unread.");
        });

        // Keep the existing Refresh action.
        refreshButton.addActionListener(e -> runRefreshAction());

        // New Update News button uses the same configured controller action.
        updateButton.addActionListener(e -> runRefreshAction());

        // ----- Clear Database -----
        clearDatabaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Warn the user: this removes everything.
                int answer = JOptionPane.showConfirmDialog(
                        NewsGUI.this,
                        "This will delete ALL saved news and deletion records.\n\n"
                                + "Click Refresh afterwards to download news again.\n\nContinue?",
                        "Clear Database",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }

                try {
                    newsCleaner.clearAll();   // empty the database
                    allNews.clear();          // empty the in-memory list
                    readKeys.clear();         // forget all read statuses
                    saveReadState();
                    newsModel.clear();        // empty the visible list
                    newsContent.setText("");  // clear the details panel
                    suggestionsPopup.setVisible(false);
                    setStatus("Database cleared. Click Update News to reload news.");

                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Database cleared successfully.\nClick Update News to download news again.",
                            "Database Cleared",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            NewsGUI.this,
                            "Could not clear the database:\n" + ex.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // ----- Search field: react to every change in the text -----
        // A DocumentListener has three methods; all of them do the same thing.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {   // characters were typed/pasted
                updateSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {   // characters were deleted
                updateSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {  // attribute change (rarely used)
                updateSearch();
            }
        });

        // ----- Language dropdown: re-filter whenever the selection changes -----
        languageBox.addActionListener(e -> {
            suggestionsPopup.setVisible(false);
            applyFilters();
        });

        // ----- List selection: show the details of the selected article -----
        newsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                // getValueIsAdjusting() is true while the selection is still
                // changing; wait until it is final to avoid duplicate updates.
                if (!e.getValueIsAdjusting()) {
                    showSelectedNews();
                }
            }
        });

        // ----- Hide the suggestions popup when the search field loses focus -----
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // invokeLater lets the focus change finish first, so clicking
                // a suggestion is not cancelled by the popup closing too early.
                SwingUtilities.invokeLater(() -> {
                    if (!searchField.hasFocus()
                            && !suggestionsPopup.isFocusOwner()) {
                        suggestionsPopup.setVisible(false);
                    }
                });
            }
        });
    }

    // Starts a news update through the controller (used by Refresh and Update News).
    private void runRefreshAction() {
        suggestionsPopup.setVisible(false);

        // An update is already running: ignore the extra click.
        if (loading) {
            return;
        }

        // No controller connected yet: explain how to fix it.
        if (refreshAction == null) {
            setStatus("Refresh action has not been configured.");
            JOptionPane.showMessageDialog(
                    this,
                    "Connect the controller using setRefreshAction(...), then try again.",
                    "Update News",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show the progress bar and disable the buttons.
        setLoading(true, "Updating news...");
        try {
            // The controller should perform the actual download in a background
            // thread and call setLoading(false, ...) when it finishes.
            refreshAction.refresh();
        } catch (RuntimeException ex) {
            // The update could not even start: undo the loading state.
            ex.printStackTrace();
            setLoading(false, "News update failed.");
            JOptionPane.showMessageDialog(
                    this,
                    "Could not start the news update:\n" + ex.getMessage(),
                    "Update Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------
    // OPEN ARTICLE
    // ------------------------------------------------------------

    // Opens the selected article's URL in the default web browser.
    private void openSelectedUrl() {
        News news = newsList.getSelectedValue();

        if (news == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select an article to open.",
                    "No Article Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String url = news.getUrl();
        if (url == null || url.trim().isEmpty()) {
            setStatus("This article has no URL.");
            return;
        }

        // Only open real web links (never file paths or other protocols).
        url = url.trim();
        if (!url.matches("(?i)^https?://\\S+$")) {
            setStatus("Cannot open this URL: " + url);
            return;
        }

        try {
            // First choice: Java's built-in Desktop API.
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                markRead(news, true);   // opened = read
                setStatus("Opened in browser: " + url);
                return;
            }

            // Fallback for Windows when the Desktop API is unavailable.
            String os = System.getProperty("os.name");
            if (os != null && os.toLowerCase(Locale.ENGLISH).contains("windows")) {
                Runtime.getRuntime().exec(new String[]{
                    "rundll32", "url.dll,FileProtocolHandler", url
                });
                markRead(news, true);   // opened = read
                setStatus("Opened in browser: " + url);
            } else {
                setStatus("Browser opening is not supported on this system.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Could not open URL: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // CONTROLLER CONNECTIONS
    // Public methods the controller (SwingAplication) calls to talk to the GUI.
    // ------------------------------------------------------------

    // Registers what should happen when the user clicks Refresh / Update News.
    public void setRefreshAction(RefreshHandler action) {
        refreshAction = action;
        updateHandlerConfigured = action != null;
    }

    // Registers what should happen when the user adds a URL.
    public void setAddUrlAction(AddUrlHandler action) {
        addUrlAction = action;
    }

    // Replaces the full article list and redraws the visible list.
    // Safe to call from any thread.
    public void setNews(List<News> news) {
        // Swing components must only be changed on the Event Dispatch Thread (EDT).
        // If we are on another thread (e.g. a download thread), copy the list
        // and re-run this method on the EDT.
        if (!SwingUtilities.isEventDispatchThread()) {
            final List<News> copy = news == null
                    ? new ArrayList<News>()
                    : new ArrayList<News>(news);
            SwingUtilities.invokeLater(() -> setNews(copy));
            return;
        }

        // Store a private copy so later changes by the caller do not affect us.
        allNews = news == null
                ? new ArrayList<News>()
                : new ArrayList<News>(news);
        applyFilters();
    }

    /**
     * Call setLoading(false, ...) from the controller when its background
     * download/database refresh completes, whether successful or not.
     *
     * While loading is true the progress bar is shown and the controls are
     * disabled, so the user cannot change anything mid-update.
     */
    public void setLoading(final boolean isLoading, final String message) {
        // Run on the EDT (see setNews).
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setLoading(isLoading, message));
            return;
        }

        loading = isLoading;

        // Show or hide the busy indicators.
        loadingBar.setVisible(isLoading);
        loadingIconLabel.setVisible(isLoading);

        // Disable every control while loading; re-enable when finished.
        refreshButton.setEnabled(!isLoading);
        updateButton.setEnabled(!isLoading);
        addButton.setEnabled(!isLoading);
        deleteButton.setEnabled(!isLoading);
        openButton.setEnabled(!isLoading);
        toggleReadButton.setEnabled(!isLoading);
        clearDatabaseButton.setEnabled(!isLoading);
        searchField.setEnabled(!isLoading);
        languageBox.setEnabled(!isLoading);

        // Show the message both in the status line and inside the bar.
        if (message != null) {
            statusLabel.setText(message);
            loadingBar.setString(message);
        }

        revalidate();
        repaint();
    }

    // Updates the status line at the bottom. Safe to call from any thread.
    public void setStatus(final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setStatus(message));
            return;
        }
        // A single space keeps the label from collapsing to zero height.
        statusLabel.setText(message == null ? " " : message);
    }

    // ------------------------------------------------------------
    // READ / UNREAD STATUS
    // The status is kept in a text file (READ_FILE), one key per line, so it
    // survives restarts without changing the database.
    // ------------------------------------------------------------

    // Unique key for an article: same pair as the database primary key.
    private String readKey(News news) {
        String source = news.getSource() == null ? "" : news.getSource().trim();
        String url = news.getUrl() == null ? "" : news.getUrl().trim();
        return source + "|" + url;
    }

    // True if the article has been read.
    private boolean isRead(News news) {
        return news != null && readKeys.contains(readKey(news));
    }

    // Sets the read status of one article, saves it and redraws the list.
    private void markRead(News news, boolean read) {
        if (news == null) {
            return;
        }

        String key = readKey(news);
        boolean changed = read ? readKeys.add(key) : readKeys.remove(key);

        if (changed) {
            saveReadState();
            newsList.repaint();                 // update the bold/gray rows
            if (news == newsList.getSelectedValue()) {
                showSelectedNews();             // update the STATUS line in the details
            }
        }
    }

    // Loads the read keys from the file (does nothing if it does not exist yet).
    private void loadReadState() {
        Path file = Paths.get(READ_FILE);
        if (!Files.exists(file)) {
            return;
        }

        try {
            readKeys.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Writes all read keys to the file.
    private void saveReadState() {
        try {
            Files.write(Paths.get(READ_FILE), readKeys, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            setStatus("Could not save read status: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // SEARCH AND AUTOCOMPLETE
    // ------------------------------------------------------------

    // Prepares text for comparison: removes accents (ă -> a, ș -> s, é -> e),
    // lower-cases it and trims spaces. Null becomes an empty string.
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        // NFD splits letters from their accent marks; \p{M} matches the marks.
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    // Returns true if the article's language matches the language chosen in the
    // dropdown. Articles with no language only appear under "All".
    private boolean languageMatches(News news) {
        String selectedLanguage = (String) languageBox.getSelectedItem();

        // "All" (or nothing selected): every article passes.
        if (selectedLanguage == null || selectedLanguage.equalsIgnoreCase("All")) {
            return true;
        }

        // A specific language is selected but the article has none (null/empty):
        // it cannot match. This is what happens when the language was never
        // saved for the article.
        String language = news.getLanguage();
        if (language == null || language.trim().isEmpty()) {
            return false;
        }

        language = normalizeText(language);

        // Accept the language name, its native name and ISO codes
        // (e.g. "ro", "ro-RO").
        switch (selectedLanguage) {
            case "Romanian":
                return language.equals("romanian")
                        || language.equals("romana")
                        || language.equals("ro")
                        || language.startsWith("ro-")
                        || language.equals("ron");

            case "English":
                return language.equals("english")
                        || language.equals("en")
                        || language.startsWith("en-");

            case "Hungarian":
                return language.equals("hungarian")
                        || language.equals("magyar")
                        || language.equals("hu")
                        || language.startsWith("hu-");

            default:
                // Any other language added to the dropdown later:
                // compare by name only.
                return language.equalsIgnoreCase(selectedLanguage);
        }
    }

    // Called every time the search text changes.
    private void updateSearch() {
        applyFilters();          // narrow the main list
        showTitleSuggestions();  // refresh the autocomplete popup
    }

    // Builds and shows the autocomplete popup under the search field:
    // up to MAX_SUGGESTIONS titles that contain the typed text.
    private void showTitleSuggestions() {
        // Start from a clean popup each time.
        suggestionsPopup.setVisible(false);
        suggestionsPopup.removeAll();

        final String query = normalizeText(searchField.getText());

        // Nothing typed, or the field is disabled (during an update): no popup.
        if (query.isEmpty() || !searchField.isEnabled()) {
            return;
        }

        int count = 0;
        for (News news : allNews) {
            // Skip invalid articles and ones hidden by the language filter.
            if (news == null || news.getTitle() == null
                    || !languageMatches(news)) {
                continue;
            }

            final String title = news.getTitle().trim();

            // Match ignoring accents and case.
            if (normalizeText(title).contains(query)) {
                JMenuItem suggestion = new JMenuItem(title);

                // Clicking a suggestion copies its title into the search field.
                suggestion.addActionListener(e -> {
                    suggestionsPopup.setVisible(false);
                    searchField.setText(title);
                    searchField.requestFocusInWindow();
                    applyFilters();
                });

                suggestionsPopup.add(suggestion);
                count++;

                // Stop once we have enough suggestions.
                if (count >= MAX_SUGGESTIONS) {
                    break;
                }
            }
        }

        // Show the popup directly below the search field, but only if there is
        // something to show and the field is actually on screen.
        if (count > 0 && searchField.isShowing()) {
            suggestionsPopup.show(searchField, 0, searchField.getHeight());
        }
    }

    // ------------------------------------------------------------
    // FILTER AND DISPLAY ARTICLES
    // ------------------------------------------------------------

    // Rebuilds the visible list from allNews, keeping only articles that match
    // BOTH the selected language and the search text.
    private void applyFilters() {
        String query = normalizeText(searchField.getText());
        String selectedLanguage = (String) languageBox.getSelectedItem();

        // Remember the current selection so we can restore it afterwards.
        News previouslySelected = newsList.getSelectedValue();

        newsModel.clear();

        for (News news : allNews) {
            if (news == null) {
                continue;
            }

            String title = normalizeText(news.getTitle());

            // An empty query matches every title, since contains("") is true.
            if (languageMatches(news) && title.contains(query)) {
                newsModel.addElement(news);
            }
        }

        // Restore the old selection if the article is still visible; otherwise
        // select the first one; if the list is empty, clear the details panel.
        if (previouslySelected != null && newsModel.contains(previouslySelected)) {
            newsList.setSelectedValue(previouslySelected, true);
        } else if (!newsModel.isEmpty()) {
            newsList.setSelectedIndex(0);
        } else {
            newsContent.setText("");
        }

        // Status line, e.g. "Showing 12 article(s) in Romanian."
        String message = "Showing " + newsModel.size() + " article(s)";
        if (selectedLanguage != null && !selectedLanguage.equals("All")) {
            message += " in " + selectedLanguage;
        }
        statusLabel.setText(message + ".");
    }

    // Fills the right-hand panel with the details of the selected article.
    private void showSelectedNews() {
        News news = newsList.getSelectedValue();
        if (news == null) {
            newsContent.setText("");
            return;
        }

        // Each section has a heading, a line of "=" and the value.
        // safeText() replaces missing values with "(Not available)".
        newsContent.setText(
                "TITLE\n"
                + "========================================\n\n"
                + safeText(news.getTitle()) + "\n\n"
                + "SOURCE\n"
                + "========================================\n\n"
                + safeText(news.getSource()) + "\n\n"
                + "STATUS\n"
                + "========================================\n\n"
                + (isRead(news) ? "Read" : "Unread") + "\n\n"
                + "DESCRIPTION\n"
                + "========================================\n\n"
                + safeText(news.getDescription()) + "\n\n"
                + "URL\n"
                + "========================================\n\n"
                + safeText(news.getUrl()));

        // Scroll back to the top (setText leaves the caret at the end).
        newsContent.setCaretPosition(0);
    }

    // Returns the text, or "(Not available)" if it is null or blank.
    private String safeText(String text) {
        return text == null || text.trim().isEmpty()
                ? "(Not available)"
                : text;
    }
}