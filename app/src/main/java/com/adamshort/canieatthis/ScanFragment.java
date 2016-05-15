package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.util.Map;

public class ScanFragment extends Fragment {

    public static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    public static final String EXTENSION = ".json";
    public static final int DOWNLOAD = 0;
    public static final int PRODUCT = 1;

    public static boolean DEBUG;
    private static boolean fragmentCreated = false;
    public static String BASE_URL = "http://world.openfoodfacts.org/api/v0/product/";
    private static String barcode = "";

    private boolean resetIntro = false;

    private static Switch dairyFreeSwitch;
    private static Switch vegetarianSwitch;
    private static Switch veganSwitch;
    private static Switch glutenFreeSwitch;
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

        switchesTableLayout = (TableLayout) view.findViewById(R.id.switchesTableLayout);

        introTextView = (TextView) view.findViewById(R.id.introTextView);
        itemTextView = (TextView) view.findViewById(R.id.itemTitleText);
        ingredientsTitleText = (TextView) view.findViewById(R.id.ingredientsTitleText);
        ingredientResponseView = (TextView) view.findViewById(R.id.ingredientsResponseView);
        tracesTitleText = (TextView) view.findViewById(R.id.tracesTitleText);
        tracesResponseView = (TextView) view.findViewById(R.id.tracesResponseView);

        Button scanButton = (Button) view.findViewById(R.id.scanButton);

        dairyFreeSwitch = (Switch) view.findViewById(R.id.dairyFreeSwitch);
        vegetarianSwitch = (Switch) view.findViewById(R.id.vegetarianSwitch);
        veganSwitch = (Switch) view.findViewById(R.id.veganSwitch);
        glutenFreeSwitch = (Switch) view.findViewById(R.id.glutenFreeSwitch);

        dairyFreeSwitch.setClickable(false);
        vegetarianSwitch.setClickable(false);
        veganSwitch.setClickable(false);
        glutenFreeSwitch.setClickable(false);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        SetItemsFromDataPasser();

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
            Log.d("setUserVisibleHint", "Fragment is visible.");
            Log.d("setUserVisibleHint", Boolean.toString(fragmentCreated));
            if (fragmentCreated) SetItemsFromDataPasser();
            resetIntro = false;
        } else {
            Log.d("setUserVisibleHint", "Fragment is not visible.");
            SetSwitchesVisibility(View.INVISIBLE);
            SetResponseItemsVisibility(View.INVISIBLE);
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
        SetItemsFromDataPasser();
    }

    //product barcode mode
    public void scanBar() {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            if (DEBUG) {
                // Digestivees
//                GetBarcodeInformation("5000168183732");
//                GetBarcodeInformation("7622210307668");
//                GetBarcodeInformation("5000168001142");
//                GetBarcodeInformation("5051140367282");
                // Muller Corner Choco Digestives
//                GetBarcodeInformation("4025500165574");
                // Jammie Dodgers
//                GetBarcodeInformation("072417143700");
                GetBarcodeInformation("790310020");
//                startActivity(new Intent(getContext(), AddProductActivity.class));
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
                            act.startActivity(intent);
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

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                //String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                if (!barcode.equals(contents)) {
                    GetBarcodeInformation(contents);
                    barcode = contents;
                }
            }
        }
    }

    public void GetBarcodeInformation(String barcode) {
        // 2nd param is output length, 3rd param is padding char
        barcode = StringUtils.leftPad(barcode, 13, "0");
        if (!ScanFragment.barcode.equals(barcode)) {
            if (((MainActivity) getActivity()).hasInternetConnection()) {
                RequestHandler rh = new RequestHandler(getContext(), progressBar, new RequestHandler.AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        JSONObject product = dataQuerier.ParseIntoJSON(output);
                        ProcessResponseFirebase(product);
                    }
                });
                rh.execute(BASE_URL + barcode + EXTENSION);
            } else {

                File products = null;
                try {
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
                            ProcessResponse(product);
                        } else {
                            ProcessResponse(null);
                        }
                    }
                } catch (FileNotFoundException e) {
                    Log.e("getBarcodeInformation", "Couldn't find file: " + e.toString());
                }
            }
        }
    }

    public void ProcessResponseFirebase(JSONObject product) {
        FirebaseAsyncRequest fbar = new FirebaseAsyncRequest();
        fbar.execute(product);
    }

    public void ProcessResponse(JSONObject product) {
        Log.d("ProcessResponse", "Product: " + product);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> editedIngredients = IngredientsList.StringToList(ingredients);
                editedIngredients = IngredientsList.RemoveUnwantedCharacters(editedIngredients, "[_]|\\s+$\"", "");
                List<String> editedTraces = IngredientsList.StringToList(traces);
                editedTraces = IngredientsList.RemoveUnwantedCharacters(editedTraces, "[_]|\\s+$\"", "");

                boolean dairy = dataQuerier.IsDairyFree(editedIngredients);
                boolean vegan = dataQuerier.IsVegan(editedIngredients);
                boolean vegetarian = true;
                // if something is vegan it is 100% vegetarian
                if (!vegan) {
                    vegetarian = dataQuerier.IsVegetarian(editedIngredients);
                }
                boolean gluten = dataQuerier.IsGlutenFree(editedIngredients);

                if (editedTraces.size() > 0) {
                    if (!editedTraces.get(0).equals("")) {
                        for (String trace : editedTraces) {
                            boolean d = dataQuerier.IsDairyFree(trace);
                            if (!d) {
                                dairy = false;
                            }
                            boolean v = dataQuerier.IsVegan(trace);
                            if (!v) {
                                vegan = false;
                            }
                            boolean ve = dataQuerier.IsVegetarian(trace);
                            if (!ve) {
                                vegetarian = false;
                            }
                            boolean g = dataQuerier.IsGlutenFree(trace);
                            if (!g) {
                                gluten = false;
                            }
                        }
                    }
                }

                SetItemTitleText(item);
                if (item.equals("")) {
                    SetItemTitleText("Product name not found");
                }
                SetDietarySwitches(dairy, vegetarian, vegan, gluten);
                SetIngredientsResponseTextBox(editedIngredients.toString().replace("[", "").replace("]", ""));
                SetTracesResponseTextBox(editedTraces.toString().replace("[", "").replace("]", ""));

                SetSwitchesVisibility(View.VISIBLE);
                introTextView.setVisibility(View.INVISIBLE);
                SetResponseItemsVisibility(View.VISIBLE);

                if (editedIngredients.size() < 1 || editedIngredients.get(0).equals("")) {
                    SetIngredientsResponseTextBox("No ingredients found");
                }
                if (editedTraces.size() < 1 || editedTraces.get(0).equals("")) {
                    SetTracesResponseTextBox("No traces found");
                }
            } else {
                showDialog(this.getActivity(), "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
            }
        } catch (JSONException e) {
            Log.e("ProcessResponse", "Issue ParseIntoJSON(response)");
        }
    }

    public void SetItemsFromDataPasser() {
        if (!resetIntro) {
            if (dataPasser == null) dataPasser = DataPasser.getInstance();

            SetDietarySwitches(dataPasser.isDairy(), dataPasser.isVegetarian(), dataPasser.isVegan(), dataPasser.isGluten());

            if (dataPasser.areSwitchesVisible()) {
                SetSwitchesVisibility(View.VISIBLE);
            } else {
                SetSwitchesVisibility(View.INVISIBLE);
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
                SetResponseItemsVisibility(View.VISIBLE);
            } else {
                SetResponseItemsVisibility(View.INVISIBLE);
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


    public void SetSwitchesVisibility(int visibility) {
        if (switchesTableLayout != null) {
            switchesTableLayout.setVisibility(visibility);
        }
        if (dairyFreeSwitch != null) {
            dairyFreeSwitch.setVisibility(visibility);
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

    public void SetResponseItemsVisibility(int visibility) {
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

    public void SetItemTitleText(String item) {
        itemTextView.setText(item);
        itemTextView.setVisibility(View.VISIBLE);
    }

    public void SetDietarySwitches(boolean dairy, boolean vegetarian, boolean vegan,
                                   boolean gluten) {
        dairyFreeSwitch.setChecked(dairy);
        vegetarianSwitch.setChecked(vegetarian);
        veganSwitch.setChecked(vegan);
        glutenFreeSwitch.setChecked(gluten);
    }

    public void SetIngredientsResponseTextBox(String response) {
        ingredientResponseView.setText(response);
        ingredientResponseView.setVisibility(View.VISIBLE);
    }

    public void SetTracesResponseTextBox(String response) {
        tracesResponseView.setText(response);
        tracesResponseView.setVisibility(View.VISIBLE);
    }

    private void queryData(DataSnapshot snapshot, JSONObject response) {
        try {
            String item = response.getString("product_name");
            String ingredients = response.getString("ingredients_text");
            String traces = response.getString("traces");

            List<String> editedIngredients = IngredientsList.StringToList(ingredients);
            editedIngredients = IngredientsList.RemoveUnwantedCharacters(editedIngredients, "[_]|\\s+$\"", "");
            List<String> editedTraces = IngredientsList.StringToList(traces);
            editedTraces = IngredientsList.RemoveUnwantedCharacters(editedTraces, "[_]|\\s+$\"", "");

            boolean lactose_free = true;
            boolean daiFalse = false;
            boolean vegetarian = true;
            boolean vegFalse = false;
            boolean vegan = true;
            boolean veganFalse = false;
            boolean gluten_free = true;
            boolean glutFalse = false;

            if (editedIngredients.size() > 0 && !editedIngredients.get(0).equals("")) {
                for (String resIngredient : editedIngredients) {
                    String lowerResIngredient = resIngredient.toLowerCase();
                    for (DataSnapshot ingredientSnapshot : snapshot.getChildren()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ing = (Map<String, Object>) ingredientSnapshot.getValue();
                        String name = ingredientSnapshot.getKey().toLowerCase();
                        if (name.contains(lowerResIngredient) || lowerResIngredient.contains(name)) {
                            if (!daiFalse) {
                                boolean dai = (Boolean) ing.get("lactose_free");
                                if (!dai) {
                                    lactose_free = false;
                                    daiFalse = true;
                                }
                            }
                            if (!vegFalse) {
                                boolean veg = (Boolean) ing.get("vegetarian");
                                if (!veg) {
                                    vegetarian = false;
                                    vegFalse = true;
                                }
                            }
                            if (!veganFalse) {
                                boolean veg = (Boolean) ing.get("vegan");
                                if (!veg) {
                                    vegan = false;
                                    veganFalse = true;
                                }
                            }
                            if (!glutFalse) {
                                boolean glu = (Boolean) ing.get("gluten_free");
                                if (!glu) {
                                    gluten_free = false;
                                    glutFalse = true;
                                }
                            }
                            Log.d("onDataChange", name + " " + lactose_free + " " + vegetarian +
                                    " " + vegan + " " + gluten_free);
                        }
                    }
                }
            }

            if (editedTraces.size() > 0 && !editedTraces.get(0).equals("")) {
                if (!editedTraces.get(0).equals("")) {
                    for (String trace : editedTraces) {
                        if (!daiFalse) {
                            boolean d = dataQuerier.IsDairyFree(trace);
                            if (!d) {
                                lactose_free = false;
                                daiFalse = true;
                            }
                        }
                        if (!veganFalse) {
                            boolean v = dataQuerier.IsVegan(trace);
                            if (!v) {
                                vegan = false;
                                veganFalse = true;
                            }
                        }
                        if (!vegFalse) {
                            boolean ve = dataQuerier.IsVegetarian(trace);
                            if (!ve) {
                                vegetarian = false;
                                veganFalse = true;
                            }
                        }
                        if (glutFalse) {
                            boolean g = dataQuerier.IsGlutenFree(trace);
                            if (!g) {
                                gluten_free = false;
                                glutFalse = true;
                            }
                        }
                    }
                }
            }

            SetItemTitleText(item);
            if (item.equals("")) {
                SetItemTitleText("Product name not found");
            }
            SetDietarySwitches(lactose_free, vegetarian, vegan, gluten_free);
            SetIngredientsResponseTextBox(editedIngredients.toString().replace("[", "").replace("]", ""));
            SetTracesResponseTextBox(editedTraces.toString().replace("[", "").replace("]", ""));

            SetSwitchesVisibility(View.VISIBLE);
            introTextView.setVisibility(View.INVISIBLE);
            SetResponseItemsVisibility(View.VISIBLE);

            if (editedIngredients.size() < 1 || editedIngredients.get(0).equals("")) {
                SetIngredientsResponseTextBox("No ingredients found");
            }
            if (editedTraces.size() < 1 || editedTraces.get(0).equals("")) {
                SetTracesResponseTextBox("No traces found");
            }

        } catch (JSONException e) {
            Log.e("ProcessResponse", "Issue ParseIntoJSON(response)");
        }
    }

    private class FirebaseAsyncRequest extends AsyncTask<JSONObject, Void, String> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(JSONObject... params) {
            Log.d("ProcessResponse", "Product: " + params[0]);
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
