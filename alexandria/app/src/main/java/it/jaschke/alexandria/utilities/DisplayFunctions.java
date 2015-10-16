package it.jaschke.alexandria.utilities;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by jeremyvalenzuela on 7/27/15.
 */
public class DisplayFunctions {
    /**
     * I got tired of typing up toasts.
     * @param context
     * @param message
     */
    public static void shortToast(Context context, String message) {
        Toast noDataToast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        noDataToast.show();
    }


}
