package com.mycompany.swingaplication;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SwingAplication {

    public static void main(String[] args) {

        // =====================================================
        // 1. CONNECT TO DATABASE
        // =====================================================

        try {
            Database.getConnection();
            System.out.println("Database connection successful!");
        } catch (Exception e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }


        // =====================================================
        // 2. LOAD RSS SOURCES FROM RSS.json
        // =====================================================

        RSSLoader loader = new RSSLoader();

        RSSparse sources = loader.loadSources();

        // This list will contain all downloaded news
        List<News> allNews = new ArrayList<>();

        RSSService rssService = new RSSService();

        NewsDAO newsDAO = new NewsDAO();


        // =====================================================
        // 3. DOWNLOAD RSS NEWS
        // =====================================================

        if (sources != null && sources.getRssSources() != null) {

            for (RSSSource source : sources.getRssSources()) {

                System.out.println(
                        "Loading: " + source.getName()
                );

                try {

                    List<News> news =
                            rssService.loadNews(source);

                    System.out.println(
                            "Found "
                            + news.size()
                            + " news from "
                            + source.getName()
                    );


                    // Go through every article
                    for (News item : news) {

                        // Add article to list for Swing
                        allNews.add(item);

                        // Save article to database
                        newsDAO.save(item);
                    }


                } catch (Exception e) {

                    System.out.println(
                            "Could not load "
                            + source.getName()
                    );

                    System.out.println(
                            e.getMessage()
                    );
                }
            }
        }


        // =====================================================
        // 4. CHECK HOW MANY NEWS WE HAVE
        // =====================================================

        System.out.println(
                "TOTAL NEWS: " + allNews.size()
        );


        // =====================================================
        // 5. CREATE WINDOW
        // =====================================================

        JFrame frame =
                new JFrame("News Application");


        // =====================================================
        // 6. CREATE NEWS LIST
        // =====================================================

        DefaultListModel<News> newsModel =
                new DefaultListModel<>();


        // Put all RSS news into the list model
        for (News news : allNews) {

            newsModel.addElement(news);
        }


        // Create JList
        JList<News> newsList =
                new JList<>();


        // Give the model to the JList
        newsList.setModel(newsModel);


        // Make the text a little easier to read
        newsList.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        14
                )
        );


        // =====================================================
        // 7. SEARCH
        // =====================================================

        JTextField searchField =
                new JTextField(20);

        JButton searchButton =
                new JButton("Search");


        searchButton.addActionListener(e -> {

            String searchText =
                    searchField
                            .getText()
                            .toLowerCase()
                            .trim();


            DefaultListModel<News> model =
                    new DefaultListModel<>();


            // Search ONLY in titles
            for (News news : allNews) {

                if (news.getTitle() != null
                        && news.getTitle()
                        .toLowerCase()
                        .contains(searchText)) {

                    model.addElement(news);
                }
            }


            newsList.setModel(model);


            // Automatically select first result
            if (model.size() > 0) {

                newsList.setSelectedIndex(0);
            }
        });


        // =====================================================
        // 8. LANGUAGE
        // =====================================================


JComboBox<String> languageBox =
        new JComboBox<>(
                new String[]{
                        "All",
                        "English",
                        "Romanian",
                        "German",
                        "French",
                        "Hungarian"
                }
        );

        // =====================================================
        // 9. TOP PANEL
        // =====================================================

        JPanel topPanel =
                new JPanel();


        topPanel.add(
                new JLabel("Language:")
        );

        topPanel.add(languageBox);


        topPanel.add(
                new JLabel("Search title:")
        );

        topPanel.add(searchField);

        topPanel.add(searchButton);


        // =====================================================
        // 10. LEFT SIDE
        // =====================================================

        JScrollPane leftPanel =
                new JScrollPane(newsList);


        // =====================================================
        // 11. RIGHT SIDE
        // =====================================================

        JTextArea newsContent =
                new JTextArea();


        newsContent.setEditable(false);

        newsContent.setLineWrap(true);

        newsContent.setWrapStyleWord(true);


        newsContent.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        15
                )
        );


        JScrollPane rightPanel =
                new JScrollPane(newsContent);


        // =====================================================
        // 12. SPLIT WINDOW
        // =====================================================

        JSplitPane splitPane =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        leftPanel,
                        rightPanel
                );


        // Width of left side
        splitPane.setDividerLocation(350);


        // =====================================================
        // 13. WHEN A NEWS ARTICLE IS SELECTED
        // =====================================================

        newsList.addListSelectionListener(e -> {

            News selectedNews =
                    newsList.getSelectedValue();


            if (selectedNews != null) {

                String title =
                        selectedNews.getTitle();

                String source =
                        selectedNews.getSource();

                String description =
                        selectedNews.getDescription();

                String url =
                        selectedNews.getUrl();


                // Put the article information
                // into the right side
                newsContent.setText(
                        "TITLE\n"
                        + "========================================\n\n"
                        + title
                        + "\n\n"

                        + "SOURCE\n"
                        + "========================================\n\n"
                        + source
                        + "\n\n"

                        + "DESCRIPTION\n"
                        + "========================================\n\n"
                        + description
                        + "\n\n"

                        + "URL\n"
                        + "========================================\n\n"
                        + url
                );


                // Start at the top
                newsContent.setCaretPosition(0);
            }
        });


        // =====================================================
        // 14. AUTOMATICALLY SELECT FIRST NEWS
        // =====================================================

        if (newsModel.size() > 0) {

            newsList.setSelectedIndex(0);
        }


        // =====================================================
        // 15. ADD PANELS TO WINDOW
        // =====================================================

        frame.add(
                topPanel,
                BorderLayout.NORTH
        );

        frame.add(
                splitPane,
                BorderLayout.CENTER
        );


        // =====================================================
        // 16. WINDOW SETTINGS
        // =====================================================

        frame.setSize(
                1000,
                650
        );


        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );


        frame.setLocationRelativeTo(null);


        frame.setVisible(true);
    }
}