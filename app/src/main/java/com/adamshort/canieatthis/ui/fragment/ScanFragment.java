package com.adamshort.canieatthis.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import com.adamshort.canieatthis.data.DataPasser;
import com.adamshort.canieatthis.data.DataQuerier;
import com.adamshort.canieatthis.util.IngredientsList;
import com.adamshort.canieatthis.util.QueryURLAsync;
import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.util.Utilities;
import com.adamshort.canieatthis.ui.activity.AddProductActivity;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
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

import static com.adamshort.canieatthis.data.DataQuerier.*;

public class ScanFragment extends Fragment {

    private static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private static final String EXTENSION = ".json";
    private static final int DOWNLOAD = 0;
    private static final int PRODUCT = 1;
    private static final int FORM_REQUEST_CODE = 11;

    public static boolean DEBUG;
    private static boolean fragmentCreated = false;
    public static String BASE_URL = "http://world.openfoodfacts.org/api/v0/product/";
    private static String barcode = "";

    private boolean resetIntro = false;

    private static Switch lactoseFreeSwitch;
    private static Switch vegetarianSwitch;
    private static Switch veganSwitch;
    private static Switch glutenFreeSwitch;
    private CoordinatorLayout coordinatorLayout;
    private TableLayout switchesTableLayout;
    private TextView introTextView;
    private TextView itemTextView;
    private TextView ingredientsTitleText;
    private TextView ingredientResponseView;
    private TextView tracesTitleText;
    private TextView tracesResponseView;
    private ProgressBar progressBar;
    private DataPasser dataPasser;
    private DataQuerier dataQuerier;

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_scan, container, false);

        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.scan_coordinator_layout);
        switchesTableLayout = (TableLayout) view.findViewById(R.id.switchesTableLayout);

        introTextView = (TextView) view.findViewById(R.id.introTextView);
        itemTextView = (TextView) view.findViewById(R.id.itemTitleText);
        ingredientsTitleText = (TextView) view.findViewById(R.id.ingredientsTitleText);
        ingredientResponseView = (TextView) view.findViewById(R.id.ingredientsResponseView);
        tracesTitleText = (TextView) view.findViewById(R.id.tracesTitleText);
        tracesResponseView = (TextView) view.findViewById(R.id.tracesResponseView);

        Button scanButton = (Button) view.findViewById(R.id.scanButton);

        lactoseFreeSwitch = (Switch) view.findViewById(R.id.lactoseFreeSwitch);
        vegetarianSwitch = (Switch) view.findViewById(R.id.vegetarianSwitch);
        veganSwitch = (Switch) view.findViewById(R.id.veganSwitch);
        glutenFreeSwitch = (Switch) view.findViewById(R.id.glutenFreeSwitch);

        lactoseFreeSwitch.setClickable(false);
        vegetarianSwitch.setClickable(false);
        veganSwitch.setClickable(false);
        glutenFreeSwitch.setClickable(false);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        setItemsFromDataPasser();

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBar();
            }
        });

        fragmentCreated = true;

        DEBUG = android.os.Debug.isDebuggerConnected();

        dataPasser = DataPasser.getInstance();

        dataQuerier = DataQuerier.getInstance(getActivity());

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
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            if (DEBUG) {
//                getBarcodeInformation("7622210307668");
                // McVities Digestives
//                getBarcodeInformation("5000168001142");
                // Tesco Orange Juice from Concentrate
//                getBarcodeInformation("5051140367282");
                // Muller Corner Choco Digestives
//                getBarcodeInformation("4025500165574");
                // Jammie Dodgers
                getBarcodeInformation("072417143700");
                // Candy Crush Candy
//                getBarcodeInformation("790310020");
                // Honey Monster Puffs
//                getBarcodeInformation("5060145250093");
//                Intent intentDebug = new Intent(getContext(), AddProductActivity.class);
//                startActivityForResult(intentDebug, FORM_REQUEST_CODE);
            } else {
                startActivityForResult(intent, 0);
            }
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(this.getActivity(), "No Scanner Found", "Download a scanner now?", "Yes", "No", DOWNLOAD).show();
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

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                //String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                getBarcodeInformation(contents);
            }
        }
        // http://stackoverflow.com/a/10407371/1860436
        else if (requestCode == 11) {
            if (resultCode == Activity.RESULT_OK) {
                Snackbar.make(coordinatorLayout, "Product was submitted successfully", Snackbar.LENGTH_LONG).show();
                setItemsFromDataPasser();
            }
        }
    }

    public void getBarcodeInformation(String barcode) {
        // 2nd param is output length, 3rd param is padding char
        barcode = StringUtils.leftPad(barcode, 13, "0");
        if (!ScanFragment.barcode.equals(barcode)) {
            ScanFragment.barcode = barcode;
            if (Utilities.hasInternetConnection(getContext())) {
                QueryURLAsync rh = new QueryURLAsync(getContext(), progressBar, new QueryURLAsync.AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        JSONObject product = dataQuerier.parseIntoJSON(output);
                        processResponseFirebase(product);
                    }
                });
                rh.execute(BASE_URL + barcode + EXTENSION);
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
    }

    public void processResponseFirebase(JSONObject product) {
        FirebaseAsyncRequest fbar = new FirebaseAsyncRequest();
        fbar.execute(product);
    }

    public void setItemsFromDataPasser() {
        if (!resetIntro) {
            if (dataPasser == null) dataPasser = DataPasser.getInstance();

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
                    ingredientResponseView.setText(dataPasser.getIngredients());
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

    private void queryData(DataSnapshot snapshot, JSONObject product) {
        Log.d("processResponse", "Product: " + product);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> ingredientsToTest = IngredientsList.stringToListAndTrim(ingredients);
                List<String> tracesToTest = IngredientsList.stringToListAndTrim(traces);

                List<String> ingredientsToDisplay = IngredientsList.stringToList(ingredients);
                List<String> tracesToDisplay = IngredientsList.stringToList(traces);

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
            ref.addValueEventListener(new ValueEventListener() {
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
}
