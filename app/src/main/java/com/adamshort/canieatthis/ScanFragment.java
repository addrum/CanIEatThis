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
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScanFragment extends Fragment {

    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    static final String BASE_URL = "http://world.openfoodfacts.org/api/v0/product/";
    static final String EXTENSION = ".json";
    static final int DOWNLOAD = 0;
    static final int PRODUCT = 1;
    static boolean DEBUG;

    public RelativeLayout responseLinearLayout;

    public TableLayout switchesTableLayout;

    public TextView introTextView;
    public TextView itemTextView;
    public TextView ingredientsTitleText;
    public TextView ingredientResponseView;
    public TextView tracesTitleText;
    public TextView tracesResponseView;

    public Button scanButton;

    public static Switch dairyFreeSwitch;
    public static Switch vegetarianSwitch;
    public static Switch veganSwitch;
    public static Switch glutenFreeSwitch;

    public void SetItemsFromDataPasser() {
        Log.d("DEBUG: ", "Dairy: " + DataPasser.getInstance().isDairy());
        Log.d("DEBUG: ", "Vegetarian: " + DataPasser.getInstance().isVegetarian());
        Log.d("DEBUG: ", "Vegan: " + DataPasser.getInstance().isVegan());
        Log.d("DEBUG: ", "Gluten: " + DataPasser.getInstance().isGluten());
        SetAllergenSwitches(DataPasser.getInstance().isDairy(), DataPasser.getInstance().isVegetarian(), DataPasser.getInstance().isVegan(), DataPasser.getInstance().isGluten());
        if (DataPasser.getInstance().areSwitchesVisible()) {
            SetSwitchesVisibility(View.VISIBLE);
        } else {
            SetSwitchesVisibility(View.INVISIBLE);
        }

        itemTextView.setText(DataPasser.getInstance().getQuery());
        if (DataPasser.getInstance().isItemVisible()) {
            itemTextView.setVisibility(View.VISIBLE);
        } else {
            itemTextView.setVisibility(View.INVISIBLE);
        }

        if (DataPasser.getInstance().isIntroVisible()) {
            introTextView.setVisibility(View.VISIBLE);
        } else {
            introTextView.setVisibility(View.INVISIBLE);
        }

        if (DataPasser.getInstance().isResponseVisible()) {
            SetResponseItemsVisibility(View.VISIBLE);
        } else {
            SetResponseItemsVisibility(View.INVISIBLE);
        }
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_main, container, false);

        responseLinearLayout = (RelativeLayout) view.findViewById(R.id.responseLinearLayout);

        switchesTableLayout = (TableLayout) view.findViewById(R.id.switchesTableLayout);

        introTextView = (TextView) view.findViewById(R.id.introTextView);
        itemTextView = (TextView) view.findViewById(R.id.itemTitleText);
        ingredientsTitleText = (TextView) view.findViewById(R.id.ingredientsTitleText);
        ingredientResponseView = (TextView) view.findViewById(R.id.ingredientsResponseView);
        tracesTitleText = (TextView) view.findViewById(R.id.tracesTitleText);
        tracesResponseView = (TextView) view.findViewById(R.id.tracesResponseView);

        scanButton = (Button) view.findViewById(R.id.scanButton);

        dairyFreeSwitch = (Switch) view.findViewById(R.id.dairyFreeSwitch);
        vegetarianSwitch = (Switch) view.findViewById(R.id.vegetarianSwitch);
        veganSwitch = (Switch) view.findViewById(R.id.veganSwitch);
        glutenFreeSwitch = (Switch) view.findViewById(R.id.glutenFreeSwitch);

        dairyFreeSwitch.setClickable(false);
        vegetarianSwitch.setClickable(false);
        veganSwitch.setClickable(false);
        glutenFreeSwitch.setClickable(false);

        SetItemsFromDataPasser();

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBar();
            }
        });

        DEBUG = android.os.Debug.isDebuggerConnected();

        return view;
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
                GetBarcodeInformation("5000295008069");
            } else {
                startActivityForResult(intent, 0);
            }
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(this.getActivity(), "No Scanner Found", "Download a scanner code activity?", "Yes", "No", DOWNLOAD).show();
        }
    }

    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo, int dialog) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        if (dialog == 0) {
            downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        act.startActivity(intent);
                    } catch (ActivityNotFoundException anfe) {
                        Log.d("ERROR", anfe.toString());
                    }
                }
            });
            downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
        }

        if (dialog == 1) {
            downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
        }

        return downloadDialog.show();
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                //String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                GetBarcodeInformation(contents);
            }
        }
    }

    public void GetBarcodeInformation(String barcode) {
        new RequestHandler().execute(BASE_URL + barcode + EXTENSION);
    }

    public void ProcessResponse(String response) {
        JSONObject product = ResponseQuerier.getInstance(this.getActivity()).ParseIntoJSON(response);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> editedIngredients = StringToList(ingredients);
                List<String> editedTraces = StringToList(traces);

                boolean dairy = ResponseQuerier.getInstance(this.getActivity()).IsDairyFree(editedIngredients);
                boolean vegetarian = ResponseQuerier.getInstance(this.getActivity()).IsVegetarian(editedIngredients);
                boolean vegan = ResponseQuerier.getInstance(this.getActivity()).IsVegan(editedIngredients);
                boolean gluten = ResponseQuerier.getInstance(this.getActivity()).IsGlutenFree(editedIngredients);

                SetItemTitleText(item);
                SetAllergenSwitches(dairy, vegetarian, vegan, gluten);
                SetIngredientsResponseTextBox(editedIngredients.toString());
                SetTracesResponseTextBox(editedTraces.toString());
                SetSwitchesVisibility(View.VISIBLE);
                introTextView.setVisibility(View.INVISIBLE);
                SetResponseItemsVisibility(View.VISIBLE);
            } else {
                showDialog(this.getActivity(), "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
            }
        } catch (JSONException e) {
            Log.d("ERROR", "Issue ParseIntoJSON(response)");
        }
    }

    public List<String> StringToList(String s) {
        ArrayList<String> ingredients = new ArrayList<>(Arrays.asList(s.split(", ")));
        for (int i = 0; i < ingredients.size(); i++) {
            ingredients.set(i, RemoveUnwantedCharacters(ingredients.get(i)));
            Log.i("INFO", ingredients.get(i));
        }
        return ingredients;
    }

    public String RemoveUnwantedCharacters(String ingredient) {
        // replace any whitespace with nothing
        return ingredient.replaceAll("[_]|\\s+$\"", "");
    }

    public void SetSwitchesVisibility(int visibility) {
        switchesTableLayout.setVisibility(visibility);
        dairyFreeSwitch.setVisibility(visibility);
        vegetarianSwitch.setVisibility(visibility);
        veganSwitch.setVisibility(visibility);
        glutenFreeSwitch.setVisibility(visibility);
    }

    public void SetResponseItemsVisibility(int visibility)
    {
        ingredientsTitleText.setVisibility(visibility);
        ingredientResponseView.setVisibility(visibility);
        tracesTitleText.setVisibility(visibility);
        tracesResponseView.setVisibility(visibility);
    }

    public void SetItemTitleText(String item) {
        itemTextView.setText(String.format(getString(R.string.product), item));
        itemTextView.setVisibility(View.VISIBLE);
    }

    public void SetAllergenSwitches(boolean dairy, boolean vegetarian, boolean vegan, boolean gluten) {
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

    public void SetItemTextViewText(String text) {
        itemTextView.setText(text);
    }

    public void SetIntroTextViewVsibility(int visibility) {
        introTextView.setVisibility(visibility);
    }

    public void SetItemTextViewVisibility(int visibility) {
        itemTextView.setVisibility(visibility);
    }

    public class RequestHandler extends AsyncTask<String, Void, String> {

        protected void onPreExecute() {
        }

        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if (response == null) {
                Log.d("ERROR", "THERE WAS AN ERROR");
                return;
            }
            Log.i("INFO", response);

            ProcessResponse(response);
        }
    }

}
