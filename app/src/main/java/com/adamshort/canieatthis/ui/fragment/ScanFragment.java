package com.adamshort.canieatthis.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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
import com.adamshort.canieatthis.data.DataQuerier;
import com.adamshort.canieatthis.ui.activity.AddProductActivity;
import com.adamshort.canieatthis.util.CSVAsync;
import com.adamshort.canieatthis.util.ListHelper;
import com.adamshort.canieatthis.util.QueryURLAsync;
import com.adamshort.canieatthis.util.Utilities;
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

import static com.adamshort.canieatthis.data.DataQuerier.*;

public class ScanFragment extends Fragment {

    private static final String EXTENSION = ".json";
    private static final int FORM_REQUEST_CODE = 11;
    private static final int SCAN_REQUEST_CODE = 49374;

    private static boolean fragmentCreated = false;
    private static String barcode = "";

    private static Menu actionMenu;
    private static CheckBox lactoseFreeSwitch;
    private static CheckBox vegetarianSwitch;
    private static CheckBox veganSwitch;
    private static CheckBox glutenFreeSwitch;

    private boolean isSearching;

    private CoordinatorLayout coordinatorLayout;
    private TableLayout switchesTableLayout;
    private TextView introTextView;
    private TextView itemTextView;
    private TextView ingredientsTitleText;
    private TextView ingredientResponseView;
    private TextView tracesTitleText;
    private TextView tracesResponseView;
    private TextView suitableTitleText;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_scan, container, false);
        setHasOptionsMenu(true);

        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.scan_coordinator_layout);
        switchesTableLayout = (TableLayout) view.findViewById(R.id.switchesTableLayout);

        introTextView = (TextView) view.findViewById(R.id.introTextView);
        itemTextView = (TextView) view.findViewById(R.id.itemTitleText);
        ingredientsTitleText = (TextView) view.findViewById(R.id.ingredientsTitleText);
        ingredientResponseView = (TextView) view.findViewById(R.id.ingredientsResponseView);
        tracesTitleText = (TextView) view.findViewById(R.id.tracesTitleText);
        tracesResponseView = (TextView) view.findViewById(R.id.tracesResponseView);
        suitableTitleText = (TextView) view.findViewById(R.id.suitableTitleText);

        Button scanButton = (Button) view.findViewById(R.id.scanButton);

        lactoseFreeSwitch = (CheckBox) view.findViewById(R.id.lactoseFreeSwitch);
        vegetarianSwitch = (CheckBox) view.findViewById(R.id.vegetarianSwitch);
        veganSwitch = (CheckBox) view.findViewById(R.id.veganSwitch);
        glutenFreeSwitch = (CheckBox) view.findViewById(R.id.glutenFreeSwitch);

        lactoseFreeSwitch.setClickable(false);
        vegetarianSwitch.setClickable(false);
        veganSwitch.setClickable(false);
        glutenFreeSwitch.setClickable(false);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.hide();
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", getString(R.string.aboutEmail), null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.issueEmailSubject));
                emailIntent.putExtra(Intent.EXTRA_TEXT,
                        "Product Name: " + itemTextView.getText() + "\n" +
                                "\nLactose Free: " + lactoseFreeSwitch.isChecked() + "\n" +
                                "Vegetarian: " + vegetarianSwitch.isChecked() + "\n" +
                                "Vegan: " + veganSwitch.isChecked() + "\n" +
                                "Gluten Free: " + glutenFreeSwitch.isChecked() + "\n" +
                                "\nIngredients: " + ingredientResponseView.getText() + "\n" +
                                "\nTraces: " + tracesResponseView.getText() + "\n" +
                                "\nDescribe any issues you are having here:");
                startActivity(Intent.createChooser(emailIntent, "Send information..."));
            }
        });

        if (!Firebase.getDefaultConfig().isPersistenceEnabled()) {
            Firebase.getDefaultConfig().setPersistenceEnabled(true);
        }
        Firebase.setAndroidContext(getContext());

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSearching) {
                    scanBar();
                    isSearching = true;
                }
            }
        });

        fragmentCreated = true;

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            Log.d("setUserVisibleHint", "ScanFragment is visible.");
            Log.d("setUserVisibleHint", Boolean.toString(fragmentCreated));
        } else {
            Log.d("setUserVisibleHint", "ScanFragment is not visible.");
            setSwitchesVisibility(View.INVISIBLE);
            setResponseItemsVisibility(View.INVISIBLE);
            if (itemTextView != null) {
                itemTextView.setVisibility(View.INVISIBLE);
            }
            if (introTextView != null) {
                introTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onViewStateRestored(Bundle inState) {
        super.onViewStateRestored(inState);
    }

    //product barcode mode
    public void scanBar() {
//            getBarcodeInformation("7622210307668");
//            McVities Digestives
//            getBarcodeInformation("5000168001142");
//            Tesco Orange Juice from Concentrate
//            getBarcodeInformation("5051140367282");
//            Muller Corner Choco Digestives
            getBarcodeInformation("4025500165574");
//            Jammie Dodgers
//            getBarcodeInformation("072417143700");
//            Candy Crush Candy
//            getBarcodeInformation("790310020");
//            Honey Monster Puffs
//            getBarcodeInformation("5060145250093");
//            Salt and Vinegar Pringles -no info but is added to db
//            getBarcodeInformation("5053990101863");
//            lemonade
//            getBarcodeInformation("0000000056434");
//            maryland cookies
//                getBarcodeInformation("072417136160");
//            fab.show();
//            go straight to add product
//            Intent intentDebug = new Intent(getContext(), AddProductActivity.class);
//            startActivityForResult(intentDebug, FORM_REQUEST_CODE);

//            IntentIntegrator.forSupportFragment(this).initiateScan();
    }

    //alert dialog for downloadDialog
    private AlertDialog showDialog(CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(title);
        dialog.setMessage(message);

        dialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    Intent intent = new Intent(getContext(), AddProductActivity.class);
                    intent.putExtra("barcode", barcode);

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
                        Snackbar.make(coordinatorLayout, "Scanning cancelled", Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(coordinatorLayout, "Barcode scanned: " + result.getContents(), Snackbar.LENGTH_LONG).show();
                        getBarcodeInformation(result.getContents());
                        if (Utilities.hasInternetConnection(getContext())) {
                            fab.show();
                        } else {
                            Log.d("onActivityResult", "No internet connection, not showing fab");
                        }
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, intent);
                }
                break;

            // http://stackoverflow.com/a/10407371/1860436
            case FORM_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Snackbar.make(coordinatorLayout, "Product was submitted successfully", Snackbar.LENGTH_LONG).show();
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

    public void getBarcodeInformation(String barcode) {
        // 2nd param is output length, 3rd param is padding char
        barcode = StringUtils.leftPad(barcode, 13, "0");
        ScanFragment.barcode = barcode;
        if (Utilities.hasInternetConnection(getContext())) {
            QueryURLAsync rh = new QueryURLAsync(getContext(), progressBar, 0, new QueryURLAsync.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    DataQuerier.getInstance(getActivity());
                    JSONObject product = DataQuerier.parseIntoJSON(output);
                    processResponseFirebase(product);
                    isSearching = false;
                }
            });
            rh.execute(getString(R.string.offBaseUrl) + barcode + EXTENSION);
        } else {
            Log.d("getBarcodeInformation", "going to try and query csv file");
            File products = null;
            try {
//                noinspection ConstantConditions
                products = new File(getContext().getExternalFilesDir(null).getPath(), "products.csv");
            } catch (NullPointerException e) {
                Log.e("getBarcodeInformation", "Couldn't open csv file: " + e.toString());
            }
            if (products != null && products.exists()) {
                CSVAsync csvAsync = new CSVAsync(barcode, progressBar, new CSVAsync.AsyncResponse() {
                    @Override
                    public void processFinish(final JSONObject output) {
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
                        isSearching = false;
                    }
                });
                csvAsync.execute(products);
            } else {
                isSearching = false;
                Log.e("getBarcodeInformation", "Couldn't find file");
                Snackbar.make(coordinatorLayout, "No offline database found", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Download", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Utilities.downloadDatabase(getActivity(), getContext());
                            }
                        })
                        .show();
            }
        }
    }

    public void processResponseFirebase(JSONObject product) {
        FirebaseAsyncRequest fbar = new FirebaseAsyncRequest();
        fbar.execute(product);
    }

    public void queryData(DataSnapshot snapshot, JSONObject product) {
        Log.d("queryData", "Product: " + product);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> ingredientsToTest = ListHelper.stringToListAndTrim(ingredients);
                List<String> tracesToTest = ListHelper.stringToListAndTrim(traces);

                List<String> ingredientsToDisplay = ListHelper.stringToList(ingredients);
                List<String> tracesToDisplay = ListHelper.stringToList(traces);

                boolean[] bools = processDataFirebase(ingredientsToTest, tracesToTest, snapshot);

                setItemTitleText(item);
                if (item.equals("")) {
                    setItemTitleText("Product name not found");
                }
                setDietarySwitches(bools[0], bools[1], bools[2], bools[3]);
                Log.d("queryData", "ingredientsToDisplay: " + ingredientsToDisplay);
                setIngredientsResponseTextBox(ingredientsToDisplay.toString().replace("[", "").replace("]", ""));
                Log.d("queryData", "tracesToDisplay: " + tracesToDisplay);
                setTracesResponseTextBox(tracesToDisplay.toString().replace("[", "").replace("]", ""));

                setSwitchesVisibility(View.VISIBLE);
                introTextView.setVisibility(View.INVISIBLE);
                setResponseItemsVisibility(View.VISIBLE);

                if (ingredientsToDisplay.size() < 1 || ingredientsToDisplay.get(0).equals("")) {
                    setIngredientsResponseTextBox(getString(R.string.noIngredientsFoundText));
                }
                if (tracesToDisplay.size() < 1 || tracesToDisplay.get(0).equals("")) {
                    setTracesResponseTextBox(getString(R.string.noTracesFound));
                }
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
            progressBar.setVisibility(View.VISIBLE);
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
                    if (response != null) {
                        queryData(snapshot, response);
                    } else {
                        showDialog("Product Not Found", "Add the product to the database?", "Yes", "No").show();
                    }
                }

                @Override
                public void onCancelled(FirebaseError error) {
                }
            });
            return "Successful firebase request";
        }

        @Override
        protected void onPostExecute(String response) {
            progressBar.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        actionMenu = menu;
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
                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            queryIngredientSearch(snapshot, query);
                        }

                        @Override
                        public void onCancelled(FirebaseError error) {
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

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void queryIngredientSearch(DataSnapshot snapshot, String ingredient) {
        boolean[] bools = processIngredientFirebase(ingredient, snapshot);

        setItemTitleText(ingredient);

        setDietarySwitches(bools[0], bools[1], bools[2], bools[3]);
        setSwitchesVisibility(View.VISIBLE);
        introTextView.setVisibility(View.INVISIBLE);

        actionMenu.findItem(R.id.action_search).collapseActionView();
    }

    @Override
    public void onPause() {
        Log.e("onPause", "OnPause of HomeFragment");
        isSearching = false;
        super.onPause();
    }

    public void setSwitchesVisibility(int visibility) {
        if (switchesTableLayout != null) {
            switchesTableLayout.setVisibility(visibility);
        }
        if (lactoseFreeSwitch != null) {
            lactoseFreeSwitch.setVisibility(visibility);
        }
        if (vegetarianSwitch != null) {
            vegetarianSwitch.setVisibility(visibility);
        }
        if (veganSwitch != null) {
            veganSwitch.setVisibility(visibility);
        }
        if (glutenFreeSwitch != null) {
            glutenFreeSwitch.setVisibility(visibility);
        }
        if (suitableTitleText != null) {
            suitableTitleText.setVisibility(visibility);
        }
    }

    public void setResponseItemsVisibility(int visibility) {
        if (ingredientsTitleText != null) {
            ingredientsTitleText.setVisibility(visibility);
        }
        if (ingredientResponseView != null) {
            ingredientResponseView.setVisibility(visibility);
        }
        if (tracesTitleText != null) {
            tracesTitleText.setVisibility(visibility);
        }
        if (tracesResponseView != null) {
            tracesResponseView.setVisibility(visibility);
        }
    }

    public void setItemTitleText(String item) {
        itemTextView.setText(item);
        itemTextView.setVisibility(View.VISIBLE);
    }

    public void setDietarySwitches(boolean lactose, boolean vegetarian, boolean vegan,
                                   boolean gluten) {
        lactoseFreeSwitch.setChecked(lactose);
        vegetarianSwitch.setChecked(vegetarian);
        veganSwitch.setChecked(vegan);
        glutenFreeSwitch.setChecked(gluten);
    }

    public void setIngredientsResponseTextBox(String response) {
        ingredientResponseView.setVisibility(View.VISIBLE);
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
            Log.d("setItemsFromDataPasser", "Regex replaced string is: " + sb);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ingredientResponseView.setText(Html.fromHtml(sb.toString(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
            } else {
                //noinspection deprecation
                ingredientResponseView.setText(Html.fromHtml(sb.toString()));
            }
        } else {
            ingredientResponseView.setText(response);
        }
    }

    public void setTracesResponseTextBox(String response) {
        tracesResponseView.setText(response);
        tracesResponseView.setVisibility(View.VISIBLE);
    }
}
