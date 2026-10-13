
package com.mycompany.swingaplication;

// Used to read data from the RSS feed
import java.io.InputStream;
import java.io.IOException;

// Used to connect to the RSS feed URL
import java.net.HttpURLConnection;
import java.net.URL;

// Used to store dates and times in SQL-compatible format
import java.sql.Timestamp;

// Used to parse dates that include a time zone
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

// Used to create and store a list of news articles
import java.util.ArrayList;
import java.util.List;

// Used to parse the XML document returned by the RSS feed
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

// Jsoup is used to extract HTML elements and clean HTML content
import org.jsoup.Jsoup;

// Classes used to navigate and read XML elements
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RSSService {

    // Downloads news articles from an RSS feed and returns them as a list.
    public List<News> loadNews(RSSSource source) throws Exception {

        // Create an empty list where the news articles will be stored.
        List<News> newsList = new ArrayList<>();

        // Get the RSS feed URL from the RSSSource object.
        URL url = new URL(source.getUrl());

        // Open a connection to the RSS feed.
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        // Send an HTTP GET request to download the RSS feed.
        connection.setRequestMethod("GET");

        // Stop waiting if the server does not respond within 10 seconds.
        connection.setConnectTimeout(10000);

        // Stop waiting if the server takes too long to send data.
        connection.setReadTimeout(10000);

        //  Get the HTTP response status code.
        int statusCode = connection.getResponseCode();

        //  Check whether the HTTP request was successful.
        if (statusCode < 200 || statusCode >= 300) {

            // Disconnect from the server before reporting the error.
            connection.disconnect();

            // Throw an exception containing the HTTP status code.
            throw new IOException("HTTP error: " + statusCode);
        }

        // Read the data returned by the server.
        InputStream inputStream = connection.getInputStream();

        try {
            // Create a factory that produces XML document parsers.
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            // Create a parser that can read XML documents.
            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            // Parse the downloaded RSS XML into a Document object.
            Document document = builder.parse(inputStream);

            // Find all <item> elements in the RSS document.
            // Each <item> normally represents one news article.
            NodeList items = document.getElementsByTagName("item");

            // Loop through all the articles found in the RSS feed.
            for (int i = 0; i < items.getLength(); i++) {

                // Get the current article as an XML node.
                Node node = items.item(i);

                // Make sure the current node is an XML element.
                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    // Convert the node into an Element.
                    Element item = (Element) node;

                    // Extract the article title.
                    String title = getElementText(item, "title");

                    // Extract the article description.
                    String rawDescription =
                            getElementText(item, "description");

                    // Extract the article URL.
                    String link = getElementText(item, "link");

                    // Extract and convert the publication date.
                    Timestamp publishDate =
                            parseDate(getElementText(item, "pubDate"));

                    // Parse the description as HTML.
                    org.jsoup.nodes.Document html =
                            Jsoup.parse(rawDescription);

                    // Find the first image in the description.
                    org.jsoup.nodes.Element img =
                            html.selectFirst("img");

                    String coverImage =
                            (img != null) ? img.attr("src") : "";

                    // Remove unwanted WordPress footer paragraphs.
                    for (org.jsoup.nodes.Element p : html.select("p")) {
                        if (p.text().startsWith("Post-ul ")) {
                            p.remove();
                        }
                    }

                    // Extract readable text from the HTML.
                    String description = html.text().trim();

                    // Create a News object containing the extracted data.
                    News news = new News(
                            title,
                            description,
                            source.getName(),
                            "",
                            "",
                            publishDate,
                            link,
                            new Timestamp(System.currentTimeMillis()),
                            new Timestamp(System.currentTimeMillis()),
                            source.getLanguage(),
                            coverImage
                    );

                    // Add the article to the list.
                    newsList.add(news);
                }
            }

            // Return all downloaded news articles.
            return newsList;

        } finally {
            // Close the stream even if parsing or processing fails.
            try {
                inputStream.close();
            } finally {
                // Always disconnect from the RSS server.
                connection.disconnect();
            }
        }
    }

    // Searches for an XML element and returns its text content.
    private String getElementText(Element parent, String tagName) {

        NodeList nodes = parent.getElementsByTagName(tagName);

        if (nodes.getLength() > 0) {
            return nodes.item(0)
                    .getTextContent()
                    .trim();
        }

        return "";
    }

    // Converts an RSS publication date into a SQL Timestamp.
    private Timestamp parseDate(String text) {
        try {
            return Timestamp.from(
                    ZonedDateTime.parse(
                            text,
                            DateTimeFormatter.RFC_1123_DATE_TIME
                    ).toInstant()
            );

        } catch (Exception e) {
            return null;
        }
    }
}
