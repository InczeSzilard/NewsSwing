
package com.mycompany.swingaplication;

// Used to read data from the RSS feed
import java.io.InputStream;

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
    // The RSSSource object contains information about the feed,
    // such as its URL, name, and language.
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

        // Stop waiting if the server takes too long to send the data.
        connection.setReadTimeout(10000);

        // Read the data returned by the server.
        InputStream inputStream = connection.getInputStream();

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

                // Convert the node into an Element
                // so we can read its child elements.
                Element item = (Element) node;

                // Extract the article title from <title>.
                String title = getElementText(item, "title");

                // Extract the article description from <description>.
                // This may contain HTML tags and an image.
                String rawDescription =
                        getElementText(item, "description");

                // Extract the article URL from <link>.
                String link = getElementText(item, "link");

                // Extract the publication date from <pubDate>
                // and convert it into a SQL Timestamp.
                Timestamp publishDate =
                        parseDate(getElementText(item, "pubDate"));

                // Parse the description as HTML so we can
                // find images and remove unwanted elements.
                org.jsoup.nodes.Document html =
                        Jsoup.parse(rawDescription);

                // Find the first image in the description.
                // If an image exists, use its src attribute
                // as the article's cover image URL.
                org.jsoup.nodes.Element img =
                        html.selectFirst("img");

                String coverImage =
                        (img != null) ? img.attr("src") : "";

                // Remove paragraphs containing the WordPress
                // footer text that starts with "Post-ul ".
                // This prevents unwanted footer text from appearing.
                for (org.jsoup.nodes.Element p : html.select("p")) {
                    if (p.text().startsWith("Post-ul ")) {
                        p.remove();
                    }
                }

                // Extract readable text from the HTML.
                // This removes HTML tags and decodes HTML entities.
                // Example: <p>Hello</p> becomes "Hello".
                String description = html.text().trim();

                // Create a News object containing the extracted data.
                News news = new News(
                        title,                              // Article title
                        description,                        // Clean description
                        source.getName(),                   // RSS source name
                        "",                                 // Empty field
                        "",                                 // Empty field
                        publishDate,                        // Publication date
                        link,                               // Article URL
                        new Timestamp(System.currentTimeMillis()), // Creation time
                        new Timestamp(System.currentTimeMillis()), // Update time
                        source.getLanguage(),               // Article language
                        coverImage                          // Cover image URL
                );

                // Add the article to the list.
                newsList.add(news);
            }
        }

        // Close the input stream after reading the RSS feed.
        inputStream.close();

        // Disconnect from the RSS server.
        connection.disconnect();

        // Return all downloaded news articles.
        return newsList;
    }

    // Searches for an XML element with the given tag name
    // and returns its text content.
    // If the element does not exist, return an empty string.
    private String getElementText(Element parent, String tagName) {

        // Find elements with the specified tag name
        // inside the current article.
        NodeList nodes = parent.getElementsByTagName(tagName);

        // Check whether at least one matching element exists.
        if (nodes.getLength() > 0) {

            // Return its text content without leading or trailing spaces.
            return nodes.item(0)
                    .getTextContent()
                    .trim();
        }

        // Return an empty string when the element is missing.
        return "";
    }

    // Converts an RSS publication date into a SQL Timestamp.
    // Example input: Sat, 10 Oct 2026 14:30:00 +0000
    private Timestamp parseDate(String text) {
        try {

            // Parse the date using the RFC 1123 date format.
            // Convert it to an Instant and then to a Timestamp.
            return Timestamp.from(
                    ZonedDateTime.parse(
                            text,
                            DateTimeFormatter.RFC_1123_DATE_TIME
                    ).toInstant()
            );

        } catch (Exception e) {

            // If the date is missing or invalid,
            // return null instead of stopping the program.
            return null;
        }
    }
}
