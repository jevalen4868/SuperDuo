package it.jaschke.alexandria;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import it.jaschke.alexandria.connector.BookConnector;
import it.jaschke.alexandria.connector.BookConnectorFactory;
import it.jaschke.alexandria.data.BookVo;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.utilities.DisplayFunctions;

/**
 * Created by jeremyvalenzuela on 11/7/15.
 */
public class AddBookConfirm extends Fragment {
    private static final String LOG_TAG = AddBookConfirm.class.getSimpleName();

    public static final String EAN_KEY = "EAN";
    private View rootView;
    private String ean;
    private ShareActionProvider shareActionProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            ean = arguments.getString(BookDetail.EAN_KEY);
        }
        rootView = inflater.inflate(R.layout.fragment_add_detail_confirm, container, false);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_detail, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // So that our menu share action provider is initialized, we do this after
        (new FetchBookInfoAsync()).execute();
    }

    private void setBookDetail(final BookVo bookVo) {
        String title = bookVo.getTitle();
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(title);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + title);
        shareActionProvider.setShareIntent(shareIntent);

        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookVo.getSubtitle());

        ((TextView) rootView.findViewById(R.id.description)).setText(bookVo.getDescription());

        String authors = bookVo.getAuthors();
        if(authors != null) {
            String[] authorsArr = authors.split(",");
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",", "\n"));
        }
        String imgUrl = bookVo.getImageLinks();
        if(imgUrl != null ) {
            if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
                Picasso.with(getActivity())
                        .load(imgUrl)
                        .into((ImageView) rootView.findViewById(R.id.bookCover));
                rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
            }
        }

        ((TextView) rootView.findViewById(R.id.categories)).setText(bookVo.getCategories());

        // Ensure visibility of Ok / Cancel buttons.
        View saveButton = rootView.findViewById(R.id.save_button);
        saveButton.setVisibility(View.VISIBLE);
        View deleteButton = rootView.findViewById(R.id.delete_button);
        deleteButton.setVisibility(View.VISIBLE);
        
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(LOG_TAG + "save_button", bookVo.getBookJson());
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.setAction(BookService.INSERT_BOOK);
                bookIntent.putExtra(BookService.JSON, bookVo.getBookJson());
                getActivity().startService(bookIntent);
                // No matter what, reset the book view.
                resetView();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // No matter what, reset the book view.
                resetView();
            }
        });
    }

    private void resetView() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment nextFragment = new AddBook();

        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                .addToBackStack(AddBookConfirm.class.getSimpleName())
                .commit();
    }

    private class FetchBookInfoAsync extends AsyncTask<Void, Void, BookVo> {
        @Override
        protected BookVo doInBackground(Void... params) {
            BookConnector connector = BookConnectorFactory.getDefaultBookConnector();
            return connector.getBookVo(ean);
        }

        @Override
        protected void onPostExecute(BookVo bookVo) {
            super.onPostExecute(bookVo);
            if(getActivity() == null) {
                return;
            }
            if(bookVo == null) {
                DisplayFunctions.shortToast(getActivity(), "No data found!");
                // No data found, return to normal view.
                resetView();
                return;
            }
            setBookDetail(bookVo);
        }
    }
}
