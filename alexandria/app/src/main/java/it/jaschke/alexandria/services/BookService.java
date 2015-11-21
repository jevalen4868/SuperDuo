package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.data.BookLabels;
import it.jaschke.alexandria.utilities.NetworkFunctions;

/**
 * CHANGE LOG
 * - Jeremy Valenzuela - 10/15/2015
 * If an invalid barcode was attempted, user was not notified. Fixed.
 */

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String INSERT_BOOK = "it.jaschke.alexandria.services.action.INSERT_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";

    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";
    // When inserting a book, we are receiving it's json.
    public static final String JSON = "it.jaschke.alexandria.services.extra.JSON";


    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(LOG_TAG, action);
            if (INSERT_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(JSON);
                insertBook(ean);
            } else if (DELETE_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                deleteBook(ean);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void deleteBook(String ean) {
        if(ean!=null) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        }
    }

    private void sendBroadcastMessageToMain(String message) {
        Log.d(LOG_TAG, message);
        Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
        messageIntent.putExtra(MainActivity.MESSAGE_KEY, message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
    }

    /**
     * Handle action insertBook in the provided background thread with the provided
     * parameters.
     */
    private void insertBook(String json) {

        Log.d(LOG_TAG, "json=" + json);

        if (json == null || json.isEmpty()) {
            sendBroadcastMessageToMain(getString(R.string.invalid_json));
            return;
        }

        try {
            // Parse json passed in.
            JSONObject bookJson = new JSONObject(json);
            JSONArray bookArray;
            if (bookJson.has(BookLabels.ITEMS)) {
                bookArray = bookJson.getJSONArray(BookLabels.ITEMS);
            } else {
                sendBroadcastMessageToMain(getString(R.string.not_found));
                return;
            }

            JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(BookLabels.VOLUME_INFO);

            // First we want to see if we already have the book.
            JSONArray industryIdentifiers = bookInfo.getJSONArray(BookLabels.INDUSTRY_IDENTIFIERS);
            String ean = "";
            for (int typeItr = 0; typeItr < industryIdentifiers.length(); typeItr++) {
                JSONObject isbnObject = industryIdentifiers.getJSONObject(typeItr);
                if ("ISBN_13".contentEquals(isbnObject.getString(BookLabels.ISBN_TYPE))) {
                    ean = isbnObject.getString(BookLabels.ISBN_IDENTIFER);
                }
            }

            Cursor bookEntry = getContentResolver().query(
                    AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                    null, // leaving "columns" null just returns all the columns.
                    null, // cols for "where" clause
                    null, // values for "where" clause
                    null  // sort order
            );

            if (bookEntry.getCount() > 0) {
                sendBroadcastMessageToMain(getString(R.string.book_exists));
                bookEntry.close();
                return;
            }

            bookEntry.close();

            String title = bookInfo.getString(BookLabels.TITLE);

            String subtitle = "";
            if (bookInfo.has(BookLabels.SUBTITLE)) {
                subtitle = bookInfo.getString(BookLabels.SUBTITLE);
            }

            String desc = "";
            if (bookInfo.has(BookLabels.DESC)) {
                desc = bookInfo.getString(BookLabels.DESC);
            }

            String imgUrl = "";
            if (bookInfo.has(BookLabels.IMG_URL_PATH) && bookInfo.getJSONObject(BookLabels.IMG_URL_PATH).has(BookLabels.IMG_URL)) {
                imgUrl = bookInfo.getJSONObject(BookLabels.IMG_URL_PATH).getString(BookLabels.IMG_URL);
            }

            writeBackBook(ean, title, subtitle, desc, imgUrl);

            if (bookInfo.has(BookLabels.AUTHORS)) {
                writeBackAuthors(ean, bookInfo.getJSONArray(BookLabels.AUTHORS));
            }
            if (bookInfo.has(BookLabels.CATEGORIES)) {
                writeBackCategories(ean, bookInfo.getJSONArray(BookLabels.CATEGORIES));
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
            sendBroadcastMessageToMain(getString(R.string.book_saved));
        }

        sendBroadcastMessageToMain(getString(R.string.book_saved));
    }

    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.TITLE, title);
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.DESC, desc);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    private void writeBackAuthors(String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values= new ContentValues();
        for (int i = 0; i < jsonArray.length(); i++) {
            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.AUTHOR, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
            values= new ContentValues();
        }
    }

    private void writeBackCategories(String ean, JSONArray jsonArray) throws JSONException {
        ContentValues values= new ContentValues();
        for (int i = 0; i < jsonArray.length(); i++) {
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
            values= new ContentValues();
        }
    }
 }