package com.adamshort.canieatthis.app.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.DataPasser;
import com.adamshort.canieatthis.app.data.DataQuerier;
import com.adamshort.canieatthis.app.ui.activity.AddProductActivity;
import com.adamshort.canieatthis.app.util.CSVReaderAsync;
import com.adamshort.canieatthis.app.util.ListHelper;
import com.adamshort.canieatthis.app.util.PreferencesHelper;
import com.adamshort.canieatthis.app.util.QueryURLAsync;
import com.adamshort.canieatthis.app.util.Utilities;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adamshort.canieatthis.app.data.DataQuerier.processDataFirebase;
import static com.adamshort.canieatthis.app.data.DataQuerier.processIngredientFirebase;

public class ScanFragment extends Fragment {

    private static final int FORM_REQUEST_CODE = 0;
    private static final int MY_PERMISSION_ACCESS_CAMERA = 1;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 2;
    private static final int SCAN_REQUEST_CODE = 49374;
    private static final String EXTENSION = ".json";

    private static boolean mFragmentCreated = false;
    private static String mBarcode = "";

    private static CheckBox mGlutenFreeSwitch;
    private static CheckBox mLactoseFreeSwitch;
    private static CheckBox mVeganSwitch;
    private static CheckBox mVegetarianSwitch;
    private static Menu mActionMenu;

    private boolean mIsSearching;

    private CoordinatorLayout mCoordinatorLayout;
    private FloatingActionButton mFab;
    private ProgressBar mProgressBar;
    private Snackbar mIngredientNotFoundSnackbar;
    private TableLayout mSwitchesTableLayout;
    private TextView mIngredientResponseView;
    private TextView mIngredientsTitleText;
    private TextView mIntroTextView;
    private TextView mProductNameTextView;
    private TextView mSuitableTitleText;
    private TextView mTracesResponseView;
    private TextView mTracesTitleText;

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_scan, container, false);
        setHasOptionsMenu(true);

        if (Utilities.isLargeDevice(getContext())) {
            mCoordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.scan_coordinator_layout);
        } else {
            mCoordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.mainActivityCoordinatorLayout);
        }
        mSwitchesTableLayout = (TableLayout) view.findViewById(R.id.switchesTableLayout);

        mIntroTextView = (TextView) view.findViewById(R.id.introTextView);
        mProductNameTextView = (TextView) view.findViewById(R.id.productNameTextView);
        mIngredientsTitleText = (TextView) view.findViewById(R.id.ingredientsTitleText);
        mIngredientResponseView = (TextView) view.findViewById(R.id.ingredientsResponseView);
        mTracesTitleText = (TextView) view.findViewById(R.id.tracesTitleText);
        mTracesResponseView = (TextView) view.findViewById(R.id.tracesResponseView);
        mSuitableTitleText = (TextView) view.findViewById(R.id.suitableTitleText);

        Button scanButton = (Button) view.findViewById(R.id.scanButton);

        mLactoseFreeSwitch = (CheckBox) view.findViewById(R.id.lactoseFreeSwitch);
        mVegetarianSwitch = (CheckBox) view.findViewById(R.id.vegetarianSwitch);
        mVeganSwitch = (CheckBox) view.findViewById(R.id.veganSwitch);
        mGlutenFreeSwitch = (CheckBox) view.findViewById(R.id.glutenFreeSwitch);

        mLactoseFreeSwitch.setClickable(false);
        mVegetarianSwitch.setClickable(false);
        mVeganSwitch.setClickable(false);
        mGlutenFreeSwitch.setClickable(false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.hide();
        mFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startEmailIntent(getString(R.string.issueEmailSubject),
                        "Barcode: " + mBarcode + "\n" +
                                "\nProduct Name: " + mProductNameTextView.getText() + "\n" +
                                "\nLactose Free: " + mLactoseFreeSwitch.isChecked() + "\n" +
                                "Vegetarian: " + mVegetarianSwitch.isChecked() + "\n" +
                                "Vegan: " + mVeganSwitch.isChecked() + "\n" +
                                "Gluten Free: " + mGlutenFreeSwitch.isChecked() + "\n" +
                                "\nIngredients: " + mIngredientResponseView.getText() + "\n" +
                                "\nTraces: " + mTracesResponseView.getText() + "\n" +
                                "\nDescribe any issues you are having here:");
            }
        });

        if (!Firebase.getDefaultConfig().isPersistenceEnabled()) {
            Firebase.getDefaultConfig().setPersistenceEnabled(true);
        }
        Firebase.setAndroidContext(getContext());

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIngredientNotFoundSnackbar != null) {
                    mIngredientNotFoundSnackbar.dismiss();
                }

                if (!mIsSearching) {
                    scanBarcode();
                    mIsSearching = true;
                }
            }
        });

        mFragmentCreated = true;

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            Log.i("setUserVisibleHint", "ScanFragment is visible.");
            Log.d("setUserVisibleHint", Boolean.toString(mFragmentCreated));
        } else {
            Log.i("setUserVisibleHint", "ScanFragment is not visible.");
            setCheckBoxesVisibility(View.INVISIBLE);
            setResponseItemsVisibility(View.INVISIBLE);
            if (mProductNameTextView != null) {
                mProductNameTextView.setVisibility(View.INVISIBLE);
            }
            if (mIntroTextView != null) {
                mIntroTextView.setVisibility(View.VISIBLE);
            }
            if (mFab != null) {
                mFab.hide();
            }
            if (mIngredientNotFoundSnackbar != null) {
                mIngredientNotFoundSnackbar.dismiss();
            }
        }
    }

    @Override
    public void onViewStateRestored(Bundle inState) {
        super.onViewStateRestored(inState);
    }

    /**
     * This method is called when we press the Scan Barcode button.
     */
    public void scanBarcode() {
//        empty product
//        getBarcodeInformation("7622210307668");
//        McVities Digestives
//        getBarcodeInformation("5000168001142");
//        Tesco Orange Juice from Concentrate
//        getBarcodeInformation("5051140367282");
//        Muller Corner Choco Digestives
//        getBarcodeInformation("4025500165574");
//        Jammie Dodgers
//        getBarcodeInformation("072417143700");
//        Candy Crush Candy
//        getBarcodeInformation("790310020");
//        Honey Monster Puffs
//        getBarcodeInformation("5060145250093");
//        Salt and Vinegar Pringles -no info but is added to db
//        getBarcodeInformation("5053990101863");
//        lemonade
//        getBarcodeInformation("0000000056434");
//        maryland cookies
//        getBarcodeInformation("072417136160");
//        fudge brownie milkshake
//        getBarcodeInformation("5000295144088");
//        go straight to add product
//        Intent intentDebug = new Intent(getContext(), AddProductActivity.class);
//        startActivityForResult(intentDebug, FORM_REQUEST_CODE);
//
        if (Utilities.hasInternetConnection(getContext())) {
            openCamera();
        } else {
            File products = getCSVIFExists();
            if (products != null) {
                openCamera();
            } else {
                showNoDatabaseFileSnackBar();
            }
        }
    }

    private void openCamera() {
        if (isCameraPermissionEnabled()) {
            IntentIntegrator.forSupportFragment(this).initiateScan();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Shows a dialog when a product is not found.
     *
     * @param title     The title of the Dialog.
     * @param message   The message to display in the Dialog.
     * @param buttonYes The label of the "yes" button.
     * @param buttonNo  The label of the "no" button.
     * @return The dialog to show.
     */
    private AlertDialog showDialog(CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(title);
        dialog.setMessage(message);

        dialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    Intent intent = new Intent(getContext(), AddProductActivity.class);
                    intent.putExtra("barcode", mBarcode);

                    try {
                        startActivityForResult(intent, FORM_REQUEST_CODE);
                    } catch (ActivityNotFoundException anfe) {
                        Log.e("showDialog", anfe.toString());
                    }
                } catch (Exception e) {
                    Log.e("showDialog", "Couldn't start new AddProductActivity");
                }
            }
        });
        dialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        return dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_REQUEST_CODE:
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                if (result != null) {
                    if (result.getContents() == null) {
                        Snackbar.make(mCoordinatorLayout, "Scanning cancelled", Snackbar.LENGTH_LONG).show();
                        mProgressBar.setVisibility(View.INVISIBLE);
                    } else {
                        Snackbar.make(mCoordinatorLayout, "Barcode scanned: " + result.getContents(), Snackbar.LENGTH_LONG).show();
                        getBarcodeInformation(result.getContents());
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, intent);
                }
                break;

            // http://stackoverflow.com/a/10407371/1860436
            case FORM_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Snackbar.make(mCoordinatorLayout, "Product was submitted successfully", Snackbar.LENGTH_LONG).show();
                    final String sProduct = intent.getStringExtra("json");
                    if (sProduct != null) {
                        Firebase ref = new Firebase(getString(R.string.firebase_url) + "/ingredients");
                        ref.keepSynced(true);
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                try {
                                    queryData(snapshot, new JSONObject(sProduct));
                                } catch (JSONException e) {
                                    Log.e("onActivityResult", "Error converting product string to json: " + e.toString());
                                }
                            }

                            @Override
                            public void onCancelled(FirebaseError error) {
                            }
                        });
                    } else {
                        Log.d("onActivityResult", "sProduct was null");
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, requestCode, intent);
        }
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d("onRequestPermissions", "Permissions have been requested");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_CAMERA:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    IntentIntegrator.forSupportFragment(this).initiateScan();
                }
                break;
            case WRITE_EXTERNAL_STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Utilities.downloadDatabase(getActivity(), WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                }
                break;
        }
    }

    // TODO both flows end up calling queryData which in turn handles firebase stuff which both flows do
    // both flows could send a message to a listener to query data instead of implementing firebase
    // and queryData in two different ways

    /**
     * Queries the mBarcode with either a GET request to Open Food Facts or uses the Open Food Facts
     * CSV dump if it has been downloaded to the device.
     *
     * @param barcode The mBarcode to query.
     */
    public void getBarcodeInformation(String barcode) {
        // 2nd param is output length, 3rd param is padding char
        barcode = StringUtils.leftPad(barcode, 13, "0");
        ScanFragment.mBarcode = barcode;
        if (Utilities.hasInternetConnection(getContext())) {
            QueryURLAsync rh = new QueryURLAsync(mProgressBar, 0, new QueryURLAsync.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    DataQuerier.getInstance();
                    JSONObject product = DataQuerier.parseIntoJSON(output);
                    processResponseFirebase(product);
                    mIsSearching = false;
                }
            });
            rh.execute(getString(R.string.offBaseAPIUrl) + barcode + EXTENSION);
        } else {
            Log.i("getBarcodeInformation", "going to try and query csv file");
            File products = getCSVIFExists();
            if (products != null) {
                CSVReaderAsync csvReaderAsync = new CSVReaderAsync(barcode, mProgressBar, getContext(), getActivity(), new CSVReaderAsync.AsyncResponse() {
                    @Override
                    public void processFinish(final JSONObject output) {
                        if (output == null) {
                            showNoDatabaseFileSnackBar();
                        } else {
                            Firebase ref = new Firebase(getString(R.string.firebase_url) + "/ingredients");
                            ref.keepSynced(true);
                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    queryData(snapshot, output);
                                }

                                @Override
                                public void onCancelled(FirebaseError error) {
                                }
                            });
                        }
                        mIsSearching = false;
                    }
                });
                csvReaderAsync.execute(products);
            } else {
                mIsSearching = false;
                showNoDatabaseFileSnackBar();
            }
        }
    }

    /**
     * Launches a new FireBaseAsyncRequest which queries the product data.
     *
     * @param product The information about the product in a JSONObject.
     */
    public void processResponseFirebase(JSONObject product) {
        FirebaseAsyncRequest fbar = new FirebaseAsyncRequest();
        fbar.execute(product);
    }

    /**
     * Queries the data fetched from the mBarcode and displays it to the user.
     *
     * @param snapshot A firebase snapshot which is passed to processDataFirebase for comparison to @values.
     * @param product  The information about the product in JSONObject format.
     */
    public void queryData(DataSnapshot snapshot, JSONObject product) {
        Log.d("queryData", "Product: " + product);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> ingredientsToTest = ListHelper.stringToListAndModify(ingredients);
                List<String> tracesToTest = ListHelper.stringToListAndModify(traces);

                List<String> ingredientsToDisplay = ListHelper.stringToList(ingredients);
                ingredientsToDisplay = ListHelper.compareTwoLists(ingredientsToDisplay, DataPasser.getFirebaseTracesList());
                List<String> tracesToDisplay = ListHelper.stringToList(traces);

                boolean[] bools = processDataFirebase(ingredientsToTest, tracesToTest, snapshot);

                setProductName(item);
                if (item.equals("")) {
                    setProductName("Product name not found");
                }
                setDietaryCheckBoxes(bools[0], bools[1], bools[2], bools[3]);
                Log.d("queryData", "ingredientsToDisplay: " + ingredientsToDisplay);
                setIngredientsResponseTextBox(ingredientsToDisplay.toString().replace("[", "").replace("]", ""));
                Log.d("queryData", "tracesToDisplay: " + tracesToDisplay);
                setTracesResponseTextBox(tracesToDisplay.toString().replace("[", "").replace("]", ""));

                setCheckBoxesVisibility(View.VISIBLE);
                mIntroTextView.setVisibility(View.INVISIBLE);
                setResponseItemsVisibility(View.VISIBLE);

                if (ingredientsToDisplay.size() < 1 || ingredientsToDisplay.get(0).equals("")) {
                    setIngredientsResponseTextBox(getString(R.string.noIngredientsFoundText));
                }
                if (tracesToDisplay.size() < 1 || tracesToDisplay.get(0).equals("")) {
                    setTracesResponseTextBox(getString(R.string.noTracesFound));
                }
                mFab.show();
            } else {
                showDialog("Product Not Found", "Add the product to the database?", "Yes", "No").show();
            }
        } catch (JSONException e) {
            Log.e("processResponse", "Issue processing response: " + e.toString());
            showDialog("Product Not Found", "Add the product to the database?", "Yes", "No").show();
        }
    }

    private class FirebaseAsyncRequest extends AsyncTask<JSONObject, Void, String> {
        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(JSONObject... params) {
            Log.d("processResponse", "Product: " + params[0]);
            final JSONObject response = params[0];
            Firebase ref = new Firebase(getString(R.string.firebase_url) + "/ingredients");
            ref.keepSynced(true);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    queryData(snapshot, response);
                }

                @Override
                public void onCancelled(FirebaseError error) {
                }
            });
            return "Successful firebase request";
        }

        @Override
        protected void onPostExecute(String response) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu, menu);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setQueryHint(getString(R.string.searchViewQueryHint));
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered
                return true;
            }

            public boolean onQueryTextSubmit(final String query) {
                if (!TextUtils.isEmpty(query)) {
                    Firebase ref = new Firebase(getString(R.string.firebase_url) + "/ingredients");
                    ref.keepSynced(true);
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (mIngredientNotFoundSnackbar != null) {
                                mIngredientNotFoundSnackbar.dismiss();
                            }
                            queryIngredientSearch(snapshot, query);
                        }

                        @Override
                        public void onCancelled(FirebaseError error) {
                            if (mIngredientNotFoundSnackbar != null) {
                                mIngredientNotFoundSnackbar.dismiss();
                            }
                        }
                    });
                }
                return true;
            }
        };

        if (searchView != null) {
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean queryTextFocused) {
                    if (!queryTextFocused) {
                        menu.findItem(R.id.action_search).collapseActionView();
                        searchView.setQuery("", false);
                    }
                }
            });

            searchView.setOnQueryTextListener(queryTextListener);
        }

        mActionMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Queries a singular ingredient.
     *
     * @param snapshot   A firebase snapshot which is also looped over for comparison to @values.
     * @param ingredient The singular ingredient to query.
     */
    private void queryIngredientSearch(DataSnapshot snapshot, final String ingredient) {
        boolean[] bools = processIngredientFirebase(ingredient, snapshot);

        setResponseItemsVisibility(View.INVISIBLE);
        mIntroTextView.setVisibility(View.INVISIBLE);

        mActionMenu.findItem(R.id.action_search).collapseActionView();

        if (bools.length > 1) {
            setCheckBoxesVisibility(View.VISIBLE);

            setProductName(ingredient);
            setDietaryCheckBoxes(bools[0], bools[1], bools[2], bools[3]);

            mFab.show();
        } else {
            setCheckBoxesVisibility(View.INVISIBLE);
            setProductName(ingredient);
            mIngredientNotFoundSnackbar = Snackbar.make(mCoordinatorLayout, "Ingredient not found", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Email", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startEmailIntent("Ingredient not Found", "Ingredient: " + ingredient + "\n\n");
                        }
                    });
            mIngredientNotFoundSnackbar.show();
        }
    }

    private File getCSVIFExists() {
        File products = null;
        try {
            // noinspection ConstantConditions
            products = new File(getContext().getExternalFilesDir(null).getPath(), "products.csv");
        } catch (NullPointerException e) {
            Log.e("getBarcodeInformation", "Couldn't open csv file: " + e.toString());
        }

        return products;
    }

    private void showNoDatabaseFileSnackBar() {
        Log.e("getBarcodeInformation", "Couldn't find file");
        Snackbar.make(mCoordinatorLayout, "No offline database found", Snackbar.LENGTH_INDEFINITE)
                .setAction("Download", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utilities.downloadDatabase(getActivity(), WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                        Snackbar.make(mCoordinatorLayout, R.string.databaseDownloadOffline, Snackbar.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void startEmailIntent(String subject, String text) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", getString(R.string.aboutEmail), null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(emailIntent, "Send information..."));
    }

    @Override
    public void onPause() {
        mIsSearching = false;
        if (mIngredientNotFoundSnackbar != null) {
            mIngredientNotFoundSnackbar.dismiss();
        }
        super.onPause();
    }

    /**
     * Checks if the user has provided permission to use their camera. Limits to asking twice.
     * After the first ask, shows a snackbar indefinitely which shows the permissions dialog again.
     */
    private boolean isCameraPermissionEnabled() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the camera permission or shows a snackbar to go to the settings if user has pressed
     * never ask again
     */
    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Need to show permission rationale, display a snackbar and then request
            // the permission again when the snackbar is dismissed.
            Snackbar.make(mCoordinatorLayout,
                    R.string.cameraPermissionDenied,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("onClick", "request permission");
                            // Request the permission again.
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    MY_PERMISSION_ACCESS_CAMERA);
                        }
                    }).show();
        } else {
            int timesAsked = PreferencesHelper.getTimesAskedForCameraPref(getContext());
            if (timesAsked < 2) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSION_ACCESS_CAMERA);
                timesAsked += 1;
                PreferencesHelper.setTimesAskedForCameraPref(getContext(), timesAsked);
            } else {
                Log.d("checkLocationPer", "don't show permission again");
                showCameraPermissionSnackbar();
            }
        }
    }

    private void showCameraPermissionSnackbar() {
        Snackbar.make(mCoordinatorLayout,
                R.string.cameraRationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction("Go to Settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent();
                        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.addCategory(Intent.CATEGORY_DEFAULT);
                        i.setData(Uri.parse("package:" + getContext().getPackageName()));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        getContext().startActivity(i);
                    }
                })
                .show();
    }

    /**
     * Sets the viibility of the dietary requirement switches.
     *
     * @param visibility The visibility state.
     */
    public void setCheckBoxesVisibility(int visibility) {
        if (mSwitchesTableLayout != null) {
            mSwitchesTableLayout.setVisibility(visibility);
        }
        if (mLactoseFreeSwitch != null) {
            mLactoseFreeSwitch.setVisibility(visibility);
        }
        if (mVegetarianSwitch != null) {
            mVegetarianSwitch.setVisibility(visibility);
        }
        if (mVeganSwitch != null) {
            mVeganSwitch.setVisibility(visibility);
        }
        if (mGlutenFreeSwitch != null) {
            mGlutenFreeSwitch.setVisibility(visibility);
        }
        if (mSuitableTitleText != null) {
            mSuitableTitleText.setVisibility(visibility);
        }
    }

    /**
     * Sets a group of views for displaying data about a product.
     *
     * @param visibility The visibility state.
     */
    public void setResponseItemsVisibility(int visibility) {
        if (mIngredientsTitleText != null) {
            mIngredientsTitleText.setVisibility(visibility);
        }
        if (mIngredientResponseView != null) {
            mIngredientResponseView.setVisibility(visibility);
        }
        if (mTracesTitleText != null) {
            mTracesTitleText.setVisibility(visibility);
        }
        if (mTracesResponseView != null) {
            mTracesResponseView.setVisibility(visibility);
        }
    }

    /**
     * Sets the text of the products name view.
     *
     * @param item The text to display.
     */
    public void setProductName(String item) {
        mProductNameTextView.setText(item);
        mProductNameTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Sets the checked state of the dietary switches.
     *
     * @param lactose    The state of the lactose free checkbox.
     * @param vegetarian The state of the vegetarian checkbox.
     * @param vegan      The state of the vegan checkbox.
     * @param gluten     The state of the gluten free checkbox.
     */
    public void setDietaryCheckBoxes(boolean lactose, boolean vegetarian, boolean vegan,
                                     boolean gluten) {
        mLactoseFreeSwitch.setChecked(lactose);
        mVegetarianSwitch.setChecked(vegetarian);
        mVeganSwitch.setChecked(vegan);
        mGlutenFreeSwitch.setChecked(gluten);
    }

    /**
     * Sets the contents of the ingredients text view. Replaces words enclosed in _ with html bold tags.
     *
     * @param response The ingredients of a product.
     */
    public void setIngredientsResponseTextBox(String response) {
        mIngredientResponseView.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(response) && !response.equals(getString(R.string.noIngredientsFoundText))) {
            Pattern pattern = Pattern.compile("(_)(\\w*)(_)");
            Matcher matcher = pattern.matcher(response);

            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String matched = matcher.group(1).replace("_", "<b>") +
                        matcher.group(2) +
                        matcher.group(3).replace("_", "</b>");
                matcher.appendReplacement(sb, matched);
            }
            matcher.appendTail(sb);

            String htmlString = sb.toString().replace("_", "")
                    .toLowerCase();
            Log.d("setItemsFromDataPasser", "Regex replaced string is: " + sb);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mIngredientResponseView.setText(Html.fromHtml(htmlString, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
            } else {
                //noinspection deprecation
                mIngredientResponseView.setText(Html.fromHtml(sb.toString().toLowerCase()));
            }
        } else {
            mIngredientResponseView.setText(response);
        }
    }

    /**
     * Sets the contents of the traces text view.
     *
     * @param response The traces of a product.
     */
    public void setTracesResponseTextBox(String response) {
        mTracesResponseView.setText(response.toLowerCase());
        mTracesResponseView.setVisibility(View.VISIBLE);
    }
}
