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
import com.adamshort.canieatthis.data.DataPasser;
import com.adamshort.canieatthis.data.DataQuerier;
import com.adamshort.canieatthis.ui.activity.AddProductActivity;
import com.adamshort.canieatthis.util.ListHelper;
import com.adamshort.canieatthis.util.QueryURLAsync;
import com.adamshort.canieatthis.util.Utilities;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adamshort.canieatthis.data.DataQuerier.processData;
import static com.adamshort.canieatthis.data.DataQuerier.processDataFirebase;
import static com.adamshort.canieatthis.data.DataQuerier.processIngredient;
import static com.adamshort.canieatthis.data.DataQuerier.processIngredientFirebase;

public class ScanFragment extends Fragment {

    private static final String EXTENSION = ".json";
    private static final int DOWNLOAD = 0;
    private static final int PRODUCT = 1;
    private static final int FORM_REQUEST_CODE = 11;
    private static final int SCAN_REQUEST_CODE = 49374;

    private static boolean fragmentCreated = false;
    private static String barcode = "";

    private boolean resetIntro = false;

    private static Menu actionMenu;
    private static CheckBox lactoseFreeSwitch;
    private static CheckBox vegetarianSwitch;
    private static CheckBox veganSwitch;
    private static CheckBox glutenFreeSwitch;
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
    private DataPasser dataPasser;
    private DataQuerier dataQuerier;
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
        dataPasser = DataPasser.getInstance(getContext());

        dataQuerier = DataQuerier.getInstance(getActivity());

        setItemsFromDataPasser();

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBar();
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
            if (fragmentCreated) setItemsFromDataPasser();
            resetIntro = false;
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
            resetIntro = true;
        }
    }

    @Override
    public void onViewStateRestored(Bundle inState) {
        super.onViewStateRestored(inState);
        setItemsFromDataPasser();
    }

    //product barcode mode
    public void scanBar() {
        if (Utilities.isInDebugMode()) {
//            getBarcodeInformation("7622210307668");
//            McVities Digestives
//            getBarcodeInformation("5000168001142");
//            Tesco Orange Juice from Concentrate
//            getBarcodeInformation("5051140367282");
//            Muller Corner Choco Digestives
//            getBarcodeInformation("4025500165574");
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
//            fab.show();
//            go straight to add product
            Intent intentDebug = new Intent(getContext(), AddProductActivity.class);
            startActivityForResult(intentDebug, FORM_REQUEST_CODE);
        } else {
            IntentIntegrator.forSupportFragment(this).initiateScan();
        }
    }

    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo, int dialogNumber) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(act);
        dialog.setTitle(title);
        dialog.setMessage(message);

        if (dialogNumber == DOWNLOAD) {
            dialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        act.startActivity(intent);
                    } catch (ActivityNotFoundException anfe) {
                        Log.e("showDialog", anfe.toString());
                    }
                }
            });
            dialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
        }

        if (dialogNumber == PRODUCT) {
            dialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        Intent intent = new Intent(act, AddProductActivity.class);
                        intent.putExtra("barcode", barcode);

                        try {
                            act.startActivityForResult(intent, FORM_REQUEST_CODE);
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
        }

        return dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SCAN_REQUEST_CODE) {
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

        }
        // http://stackoverflow.com/a/10407371/1860436
        else if (requestCode == FORM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Snackbar.make(coordinatorLayout, "Product was submitted successfully", Snackbar.LENGTH_LONG).show();
                setItemsFromDataPasser();
            }
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
                    JSONObject product = dataQuerier.parseIntoJSON(output);
                    processResponseFirebase(product);
                }
            });
            rh.execute(getString(R.string.offBaseUrl) + barcode + EXTENSION);
        } else {

            File products = null;
            try {
                //noinspection ConstantConditions
                products = new File(getContext().getExternalFilesDir(null).getPath(), "products.csv");
            } catch (NullPointerException e) {
                Log.e("getBarcodeInformation", "Couldn't open csv file: " + e.toString());
            }
            try {
                if (products != null) {
                    CsvParserSettings settings = new CsvParserSettings();
                    settings.getFormat().setDelimiter('\t');
                    settings.setMaxCharsPerColumn(10000);
                    // limits to barcode, name, ingredients and traces
                    settings.selectIndexes(0, 7, 34, 35);

                    CsvParser parser = new CsvParser(settings);

                    // call beginParsing to read records one by one, iterator-style.
                    parser.beginParsing(new FileReader(products));

                    String[] info = null;
                    String[] row;
                    while ((row = parser.parseNext()) != null) {
                        if (StringUtils.leftPad(row[0], 13, "0").equals(barcode)) {
                            info = row;
                            parser.stopParsing();
                        }
                    }
                    if (info != null) {
                        Log.d("getBarcodeInformation", Arrays.toString(info));
                        JSONObject product = new JSONObject();
                        try {
                            int length = info.length;
                            if (length > 0 && info[0] != null) {
                                product.put("barcode", info[0]);
                            } else {
                                product.put("barcode", "");
                            }
                            if (length > 1 && info[1] != null) {
                                product.put("product_name", info[1]);
                            } else {
                                product.put("product_name", "");
                            }
                            if (length > 2 && info[2] != null) {
                                product.put("ingredients_text", info[2]);
                            } else {
                                product.put("ingredients_text", "");
                            }
                            if (length > 3 && info[3] != null) {
                                product.put("traces", info[3]);
                            } else {
                                product.put("traces", "");
                            }
                        } catch (JSONException e) {
                            Log.e("getBarcodeInformation", "Couldn't create jsonobject: " + e.toString());
                        }
                        queryData(null, product);
                    } else {
                        queryData(null, null);
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e("getBarcodeInformation", "Couldn't find file: " + e.toString());
            }
        }
    }

    public void processResponseFirebase(JSONObject product) {
        FirebaseAsyncRequest fbar = new FirebaseAsyncRequest();
        fbar.execute(product);
    }

    private void queryData(DataSnapshot snapshot, JSONObject product) {
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

                boolean[] bools;
                if (snapshot != null) {
                    bools = processDataFirebase(ingredientsToTest, tracesToTest, snapshot);
                } else {
                    bools = processData(ingredientsToTest, tracesToTest);
                }

                setItemTitleText(item);
                if (item.equals("")) {
                    setItemTitleText("Product name not found");
                }
                setDietarySwitches(bools[0], bools[1], bools[2], bools[3]);
                setIngredientsResponseTextBox(ingredientsToDisplay.toString().replace("[", "").replace("]", "")
                        .replace("_", ""));
                setTracesResponseTextBox(tracesToDisplay.toString().replace("[", "").replace("]", ""));

                setSwitchesVisibility(View.VISIBLE);
                introTextView.setVisibility(View.INVISIBLE);
                setResponseItemsVisibility(View.VISIBLE);

                if (ingredientsToDisplay.size() < 1 || ingredientsToDisplay.get(0).equals("")) {
                    setIngredientsResponseTextBox("No ingredients found");
                }
                if (tracesToDisplay.size() < 1 || tracesToDisplay.get(0).equals("")) {
                    setTracesResponseTextBox("No traces found");
                }
            } else {
                showDialog(this.getActivity(), "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
            }
        } catch (JSONException e) {
            Log.e("processResponse", "Issue processing response: " + e.toString());
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
                        showDialog(getActivity(), "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
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
        boolean[] bools;
        if (snapshot != null) {
            bools = processIngredientFirebase(ingredient, snapshot);
        } else {
            bools = processIngredient(ingredient);
        }
        setItemTitleText(ingredient);

        setDietarySwitches(bools[0], bools[1], bools[2], bools[3]);
        setSwitchesVisibility(View.VISIBLE);
        introTextView.setVisibility(View.INVISIBLE);

        actionMenu.findItem(R.id.action_search).collapseActionView();
    }

    public void setItemsFromDataPasser() {
        if (!resetIntro) {
            if (dataPasser == null) dataPasser = DataPasser.getInstance(getContext());

            setDietarySwitches(dataPasser.isDairy(), dataPasser.isVegetarian(), dataPasser.isVegan(), dataPasser.isGluten());

            if (dataPasser.areSwitchesVisible()) {
                setSwitchesVisibility(View.VISIBLE);
            } else {
                setSwitchesVisibility(View.INVISIBLE);
            }

            if (itemTextView != null) {
                itemTextView.setText(dataPasser.getQuery());
                if (dataPasser.isItemVisible()) {
                    itemTextView.setVisibility(View.VISIBLE);
                } else {
                    itemTextView.setVisibility(View.INVISIBLE);
                }
            }

            if (introTextView != null) {
                if (dataPasser.isIntroVisible()) {
                    introTextView.setVisibility(View.VISIBLE);
                } else {
                    introTextView.setVisibility(View.INVISIBLE);
                }
            }

            if (dataPasser.isResponseVisible()) {
                setResponseItemsVisibility(View.VISIBLE);
            } else {
                setResponseItemsVisibility(View.INVISIBLE);
            }

            if (!dataPasser.isFromSearch()) {
                if (ingredientResponseView != null) {
                    String ingredients = dataPasser.getIngredients();
                    if (!TextUtils.isEmpty(ingredients)) {
                        Pattern pattern = Pattern.compile("(_)(\\w*)(_)");
                        Matcher matcher = pattern.matcher(ingredients);
                        StringBuffer sb = new StringBuffer();
                        while (matcher.find()) {
                            String matched = matcher.group(1).replace("_", "<b>")
                                    + matcher.group(2)
                                    + matcher.group(3).replace("_", "</b>");
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
                    }
                }
                if (tracesResponseView != null) {
                    if (dataPasser.getTraces() != null) {
                        if (!dataPasser.getTraces().equals("")) {
                            tracesResponseView.setText(dataPasser.getTraces());
                        }
                    }
                }
            }
        }
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
        ingredientResponseView.setText(response);
        ingredientResponseView.setVisibility(View.VISIBLE);
    }

    public void setTracesResponseTextBox(String response) {
        tracesResponseView.setText(response);
        tracesResponseView.setVisibility(View.VISIBLE);
    }
}
