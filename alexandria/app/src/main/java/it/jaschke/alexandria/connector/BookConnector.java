package it.jaschke.alexandria.connector;

import it.jaschke.alexandria.data.BookVo;

/**
 * Created by jeremyvalenzuela on 11/7/15.
 */
public interface BookConnector {
    public BookVo getBookVo(String ean);
}
