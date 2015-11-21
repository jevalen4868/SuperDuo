package it.jaschke.alexandria.connector;

/**
 * Created by jeremyvalenzuela on 11/7/15.
 */
public class BookConnectorFactory {
    public static BookConnector getDefaultBookConnector() {
        return new GoogleBooksConnector();
    }
}
