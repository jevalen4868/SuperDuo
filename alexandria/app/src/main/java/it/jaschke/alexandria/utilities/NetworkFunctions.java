package it.jaschke.alexandria.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by jeremyvalenzuela on 7/27/15.
 */
public class NetworkFunctions {
    /**
     * Simple helper function for checking network status.
     * @param context
     * @return if network available.
     */
    public static boolean isNetworkAvailable(Context context) {
        //Based on a stackoverflow snippet
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
