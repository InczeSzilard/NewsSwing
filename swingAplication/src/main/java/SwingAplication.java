package com.mycompany.swingaplication;
import javax.swing.*; 
import java.awt.*;




public class SwingAplication {

   public static void main(String[] args) {

    // Creating the main window
    JFrame frame = new JFrame("News Application");

    // Creating a list of news titles
    String[] newsTitle = {
        "First News Article",
        "Second News Article",
        "Third News Article",
        "Fourth News Article"
    };

    // JList displays the news titles
    JList<String> newsList = new JList<>(newsTitle);

    // Adding a search bar for the titles
    JTextField searchField = new JTextField(20);
    JButton searchButton = new JButton("Search");

    // Search only through the news titles
    searchButton.addActionListener(e -> {

        String searchText = searchField.getText().toLowerCase();

        DefaultListModel<String> model = new DefaultListModel<>();

        for (String title : newsTitle) {

            if (title.toLowerCase().contains(searchText)) {
                model.addElement(title);
            }
        }

        newsList.setModel(model);
    });

    // Creating a language field
    JComboBox<String> languageBox = new JComboBox<>(
        new String[]{"English", "Romanian", "German", "French"}
    );

    // Creating the top panel
    JPanel topPanel = new JPanel();

    topPanel.add(new JLabel("Language:"));
    topPanel.add(languageBox);

    topPanel.add(new JLabel("Search title:"));
    topPanel.add(searchField);
    topPanel.add(searchButton);

    // Put the list inside a scrollable panel
    JScrollPane leftPanel = new JScrollPane(newsList);

    // Create the area where the selected news will be displayed
    JTextArea newsContent = new JTextArea();

    newsContent.setEditable(false);
    newsContent.setLineWrap(true);
    newsContent.setWrapStyleWord(true);

    // Put the news content inside a scrollable panel
    JScrollPane rightPanel = new JScrollPane(newsContent);

    // Create the two-sided window
    JSplitPane splitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        leftPanel,
        rightPanel
    );

    // Make the left side smaller than the right side
    splitPane.setDividerLocation(200);

    // When the user selects a news title
    newsList.addListSelectionListener(e -> {

        if (!e.getValueIsAdjusting()) {

            String selectedNews = newsList.getSelectedValue();

            if (selectedNews != null) {

                newsContent.setText(
                    "You selected:\n\n"
                    + selectedNews
                    + "\n\nNews content will appear here."
                );
            }
        }
    });

    // Add the panels to the window
    frame.add(topPanel, BorderLayout.NORTH);
    frame.add(splitPane, BorderLayout.CENTER);

    // Set window size
    frame.setSize(900, 600);

    // Close the application when X is pressed
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Center the window
    frame.setLocationRelativeTo(null);

    // Show the window
    frame.setVisible(true);
}
}
