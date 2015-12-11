package it.jaschke.alexandria;

/**
 * CHANGE LOG
 * 10/15/2015 - Jeremy Valenzuela
 * - Implemented Scan functionality.
 *
 * Source:
 * https://github.com/googlesamples/android-vision/blob/master/visionSamples/barcode-reader/app/src/main/java/com/google/android/gms/samples/vision/barcodereader/BarcodeCaptureActivity.java
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import it.jaschke.alexandria.barcode.BarcodeGraphic;
import it.jaschke.alexandria.barcode.BarcodeTrackerFactory;
import it.jaschke.alexandria.camera.CameraSource;
import it.jaschke.alexandria.camera.CameraSourcePreview;
import it.jaschke.alexandria.camera.GraphicOverlay;
import it.jaschke.alexandria.utilities.DisplayFunctions;


public class AddBook extends Fragment  {
    private static final String LOG_TAG = AddBook.class.getSimpleName();
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText mEan;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    // constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String BarcodeObject = "Barcode";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    private BarcodeDetector mBarcodeDetector;
    private CameraSourcePreview mCameraSourcePreview;
    private CameraSource mCameraSource;
    private GraphicOverlay mGraphicOverlay;

    private static boolean mInstructionSnackbarShown = false;

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mEan !=null) {
            outState.putString(EAN_CONTENT, mEan.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        mEan = (EditText) rootView.findViewById(R.id.ean);
        // If landscape view this will be null.
        if(mEan != null) {
            mEan.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    //no need
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    //no need
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String ean = s.toString();
                    //catch isbn10 numbers
                    if (ean.length() == 10 && !ean.startsWith("978")) {
                        ean = "978" + ean;
                    }
                    if (ean.length() < 13) {
                        return;
                    }
                    startBookDetail(ean);
                }
            });


            rootView.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startBookDetail(mEan.getText().toString());
                }
            });
        }
        if(savedInstanceState!=null && mEan != null){
            mEan.setText(savedInstanceState.getString(EAN_CONTENT));
            mEan.setHint("");
        }

        // Only start barcode detection process if we have a camera.
        PackageManager packageManager = getActivity().getPackageManager();
        if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // read parameters from the intent used to launch the activity.
            boolean autoFocus = getActivity().getIntent().getBooleanExtra(AutoFocus, true);
            boolean useFlash = getActivity().getIntent().getBooleanExtra(UseFlash, false);

            mCameraSourcePreview = (CameraSourcePreview)rootView.findViewById(R.id.camera_source_preview);
            mGraphicOverlay = (GraphicOverlay) rootView.findViewById(R.id.graphic_overlay);

            mCameraSourcePreview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        onTap(event.getX(), event.getY());
                    }
                    return true;
                }
            });

            // Check for the camera permission before accessing the camera.  If the
            // permission is not granted yet, request permission.
            int rc = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                (new StartCameraSourceAsync()).execute(autoFocus, useFlash);
            } else {
                requestCameraPermission();
            }

            if(mInstructionSnackbarShown == false) {
                Snackbar.make(mGraphicOverlay, "After barcode recognized, tap it to capture. Or, enter ISBN manually.",
                        Snackbar.LENGTH_LONG)
                        .show();
                mInstructionSnackbarShown = true;
            }
        }

        return rootView;
    }

    private void startBookDetail(String ean) {
        if(ean.length()==10 && !ean.startsWith("978")){
            ean="978"+ean;
        }

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment addBookConfirm = new AddBookConfirm();
        Bundle bundle = new Bundle();
        bundle.putString(AddBookConfirm.EAN_KEY, ean);
        addBookConfirm.setArguments(bundle);
        fragmentManager.beginTransaction()
                .replace(R.id.container, addBookConfirm)
                .addToBackStack(AddBook.class.getSimpleName())
                .commit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     *
     */
    public class StartCameraSourceAsync extends AsyncTask<Boolean, Void, Void> {
        /**
         * Does the camera dirty work.
         * @param params [0]= autoFocus, [1]= useFlash
         * @return
         */
        @SuppressWarnings("ResourceType")
        @Override
        protected Void doInBackground(Boolean... params) {
            Context context = getActivity();
            Boolean autoFocus = params[0];
            Boolean useFlash = params[1];
            // A barcode detector is created to track barcodes.  An associated multi-processor instance
            // is set to receive the barcode detection results, track the barcodes, and maintain
            // graphics for each barcode on screen.  The factory is used by the multi-processor to
            // create a separate tracker instance for each barcode.
            BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
            BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay);
            barcodeDetector.setProcessor(
                    new MultiProcessor.Builder<>(barcodeFactory).build());

            if (!barcodeDetector.isOperational()) {
                // Note: The first time that an app using the barcode or face API is installed on a
                // device, GMS will download a native libraries to the device in order to do detection.
                // Usually this completes before the app is run for the first time.  But if that
                // download has not yet completed, then the above call will not detect any barcodes
                // and/or faces.
                //
                // isOperational() can be used to check if the required native libraries are currently
                // available.  The detectors will automatically become operational once the library
                // downloads complete on device.
                Log.w(LOG_TAG, "Detector dependencies are not yet available.");

                // Check for low storage.  If there is low storage, the native library will not be
                // downloaded, so detection will not become operational.
                IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = getActivity().registerReceiver(null, lowstorageFilter) != null;

                if (hasLowStorage) {
                    DisplayFunctions.shortToast(getActivity(), getString(R.string.low_storage_error));
                    Log.w(LOG_TAG, getString(R.string.low_storage_error));
                }
            }

            // Creates and starts the camera.  Note that this uses a higher resolution in comparison
            // to other detection examples to enable the barcode detector to detect small barcodes
            // at long distances.
            CameraSource.Builder builder = new CameraSource.Builder(getActivity(), barcodeDetector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1600, 1024)
                    .setRequestedFps(5.0f);

            // make sure that auto focus is an available option
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                builder = builder.setFocusMode(
                        autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
            }

            mCameraSource = builder
                    .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                    .build();
            // check that the device has play services available.
            int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                    getActivity());
            if (code != ConnectionResult.SUCCESS) {
                Dialog dlg =
                        GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
                dlg.show();
            }

            if (mCameraSource != null) {
                try {
                    mCameraSourcePreview.start(mCameraSource, mGraphicOverlay);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Unable to start camera source.", e);
                    mCameraSource.release();
                    mCameraSource = null;
                }
            }
            return null;
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(LOG_TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(getActivity(), permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = getActivity();

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            boolean autoFocus = getActivity().getIntent().getBooleanExtra(AutoFocus, false);
            boolean useFlash = getActivity().getIntent().getBooleanExtra(UseFlash, false);
            (new StartCameraSourceAsync()).execute(autoFocus, useFlash);
            return;
        }

        Log.e(LOG_TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                getActivity().finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }



    /**
     * onTap is called to capture the oldest barcode currently detected and
     * return it to the caller.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private boolean onTap(float rawX, float rawY) {
        Log.d(LOG_TAG, "rawX=" + rawX + ",rawY=" + rawY);
        // Store the selected graphic.
        Barcode selectedBarcode = null;
        // Get all the graphics.
        Set<BarcodeGraphic> graphics = (Set<BarcodeGraphic>)mGraphicOverlay.getAllGraphics();
        if (graphics != null) {
            Iterator<BarcodeGraphic> barcodeGraphicIterator = graphics.iterator();
            while (barcodeGraphicIterator.hasNext()) {
                BarcodeGraphic barcodeGraphic = barcodeGraphicIterator.next();
                Barcode barcode = barcodeGraphic.getBarcode();
                if (barcode != null) {
                    // Now determine which barcode the user selected.
                    RectF rect = new RectF(barcode.getBoundingBox());
                    rect.left = barcodeGraphic.translateX(rect.left);
                    rect.top = barcodeGraphic.translateY(rect.top);
                    rect.right = barcodeGraphic.translateX(rect.right);
                    rect.bottom = barcodeGraphic.translateY(rect.bottom);
                    // Compare x values.
                    boolean xHit = false;
                    if(rawX > rect.left && rawX < rect.right) { xHit = true; }
                    boolean yHit = false;
                    if( rawY > rect.top && rawY < rect.bottom) { yHit = true; }
                    if(xHit && yHit) { selectedBarcode = barcode; }
                    Log.d(LOG_TAG, "barcode=" + barcode.displayValue);
                    Log.d(LOG_TAG, "left=" + rect.left + ",top=" + rect.top + ",right=" + rect.right + ",bottom=" + rect.bottom);
                } else {
                    Log.d(LOG_TAG, "barcode data is null");
                }
            }
        }
        else {
            Log.d(LOG_TAG,"no barcode detected");
        }
        if(selectedBarcode != null) {
            Log.d(LOG_TAG, "selectedBarcode=" + selectedBarcode.displayValue);
            startBookDetail(selectedBarcode.displayValue);
        }
        return selectedBarcode != null;
    }

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mCameraSource != null) {
            try {
                mCameraSourcePreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mCameraSourcePreview != null) {
            mCameraSourcePreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraSourcePreview != null) {
            mCameraSourcePreview.release();
        }
    }
}
