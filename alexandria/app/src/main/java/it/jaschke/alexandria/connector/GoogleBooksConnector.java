package it.jaschke.alexandria.connector;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.BookLabels;
import it.jaschke.alexandria.data.BookVo;
import it.jaschke.alexandria.utilities.NetworkFunctions;

/**
 * Created by jeremyvalenzuela on 11/7/15.
 */
public class GoogleBooksConnector implements BookConnector {
    private static final String LOG_TAG = GoogleBooksConnector.class.getSimpleName();

    @Override
    public BookVo getBookVo(String ean) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String bookJsonString = null;

        try {
            final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
            final String QUERY_PARAM = "q";

            final String ISBN_PARAM = "isbn:" + ean;

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                    .build();

            URL url = new URL(builtUri.toString());

            Log.d(LOG_TAG, url.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            bookJsonString = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            Log.d(LOG_TAG, bookJsonString);
            JSONObject bookJson = new JSONObject(bookJsonString);
            JSONArray bookArray;
            if (bookJson.has(BookLabels.ITEMS)) {
                bookArray = bookJson.getJSONArray(BookLabels.ITEMS);
            } else {
                // what do say when no info? Let front end handle probably.
                return null;
            }

            JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(BookLabels.VOLUME_INFO);
            BookVo bookVo = new BookVo();
            bookVo.setBookJson(bookJsonString);
            bookVo.setTitle(bookInfo.getString(BookLabels.TITLE));

            if (bookInfo.has(BookLabels.SUBTITLE)) {
                bookVo.setSubtitle(bookInfo.getString(BookLabels.SUBTITLE));
            }

            if(bookInfo.has(BookLabels.AUTHORS)) {
                JSONArray authors = bookInfo.getJSONArray(BookLabels.AUTHORS);
                StringBuilder authorsCommaDelimited = new StringBuilder(authors.getString(0));
                // Start at one so we only add comma if necessary.
                for(int i = 1; i < authors.length(); i++) {
                    authorsCommaDelimited.append(",");
                    authorsCommaDelimited.append(authors.getString(i));
                }
                bookVo.setAuthors(authorsCommaDelimited.toString());
            }

            if (bookInfo.has(BookLabels.DESC)) {
                bookVo.setDescription(bookInfo.getString(BookLabels.DESC));
            }

            if (bookInfo.has(BookLabels.IMG_URL_PATH) && bookInfo.getJSONObject(BookLabels.IMG_URL_PATH).has(BookLabels.IMG_URL)) {
                bookVo.setImageLinks(bookInfo.getJSONObject(BookLabels.IMG_URL_PATH).getString(BookLabels.IMG_URL));
            }

            return bookVo;
        }
        catch(Exception ex) {
            Log.e(LOG_TAG, "ERROR!", ex);
        }
        return null;
    }
}
