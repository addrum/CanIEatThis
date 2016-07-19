package com.adamshort.canieatthis.app.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.DataPasser;
import com.adamshort.canieatthis.app.data.DataQuerier;
import com.adamshort.canieatthis.app.util.ListHelper;
import com.adamshort.canieatthis.app.util.QueryURLAsync;
import com.adamshort.canieatthis.app.util.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class AddProductActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://world.openfoodfacts.org/cgi/product_jqm2.pl?";

    private static String mBarcode = "";

    private List<String> mWrittenTraces;
    private String mBarcodeText;
    private String mEnergyPerText;
    private String mIngredientsText;
    private String mPortionText;
    private String mProductNameText;
    private String mQuantityText;
    private String mTracesText;
    private String mUnitText;

    private MultiAutoCompleteTextView mIngredientsTextView;
    private MultiAutoCompleteTextView mTracesTextView;
    private TextView mBarcodeNumberTextView;
    private TextView mProductNameTextView;
    private CheckBox mEnergyPer100CheckBox;
    private CheckBox mEnergyPerServingCheckBox;
    private TextView mEnergyPerTextView;
    private TextView mPortionTextView;
    private TextView mQuantityTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            mBarcode = b.getString("barcode");
        }

        DataQuerier.getInstance();

        mIngredientsTextView = (MultiAutoCompleteTextView) findViewById(R.id.input_ingredients);
        DataPasser.getInstance(getBaseContext());
        ArrayAdapter<String> ingredientsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                DataPasser.getFirebaseIngredientsList());
        mIngredientsTextView.setAdapter(ingredientsAdapter);
        mIngredientsTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        mTracesTextView = (MultiAutoCompleteTextView) findViewById(R.id.input_traces);
        ArrayAdapter<String> tracesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                DataPasser.getFirebaseTracesList());
        mTracesTextView.setAdapter(tracesAdapter);
        mTracesTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        mBarcodeNumberTextView = (TextView) findViewById(R.id.input_barcode_number);
        mProductNameTextView = (TextView) findViewById(R.id.input_product_name);
        mQuantityTextView = (TextView) findViewById(R.id.input_quantity);
        mEnergyPerTextView = (TextView) findViewById(R.id.input_energy_per);
        mEnergyPerServingCheckBox = (CheckBox) findViewById(R.id.input_energy_per_serving);
        mEnergyPer100CheckBox = (CheckBox) findViewById(R.id.input_energy_per_100g);
        mPortionTextView = (TextView) findViewById(R.id.input_portion);

        mEnergyPerServingCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEnergyPer100CheckBox.isChecked()) {
                    mEnergyPer100CheckBox.setChecked(false);
                }
                mEnergyPerTextView.setError(null);
                mPortionTextView.setEnabled(!mPortionTextView.isEnabled());
            }
        });
        mEnergyPer100CheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEnergyPerServingCheckBox.isChecked()) {
                    mEnergyPerServingCheckBox.setChecked(false);
                }
                mEnergyPerTextView.setError(null);
                mPortionTextView.setEnabled(false);
                mPortionTextView.setError(null);
            }
        });

        if (mProductNameTextView.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        final Spinner unitSpinner = (Spinner) findViewById(R.id.input_unit);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.unitValues, android.R.layout.simple_spinner_dropdown_item);
        if (unitSpinner != null) {
            unitSpinner.setAdapter(adapter);
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        Button submitProductButton = (Button) findViewById(R.id.product_submit_button);

        mBarcodeNumberTextView.setText(mBarcode);

        if (submitProductButton != null) {
            submitProductButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBarcodeText = mBarcodeNumberTextView.getText().toString();
                    mProductNameText = mProductNameTextView.getText().toString();
                    mQuantityText = mQuantityTextView.getText().toString();
                    if (unitSpinner != null) {
                        mUnitText = unitSpinner.getSelectedItem().toString();
                    }
                    mEnergyPerText = mEnergyPerTextView.getText().toString();
                    mIngredientsText = mIngredientsTextView.getText().toString();
                    mTracesText = mTracesTextView.getText().toString();
                    mPortionText = mPortionTextView.getText().toString();

                    List<TextView> required = new ArrayList<>();
                    required.add(mBarcodeNumberTextView);
                    required.add(mProductNameTextView);
                    required.add(mIngredientsTextView);

                    boolean wereErrors = false;

                    for (TextView req : required) {
                        if (req.getText().toString().isEmpty()) {
                            setErrorHints(req);
                            wereErrors = true;
                        }
                    }

                    if (!mEnergyPerText.equals("") && (!mEnergyPerServingCheckBox.isChecked() &&
                            !mEnergyPer100CheckBox.isChecked())) {
                        setErrorHints(mEnergyPerTextView, getString(R.string.noEnergyCheckedError));
                        wereErrors = true;
                    }

                    if (mEnergyPerText.equals("") && (mEnergyPerServingCheckBox.isChecked() ||
                            mEnergyPer100CheckBox.isChecked())) {
                        setErrorHints(mEnergyPerTextView, getString(R.string.energyError));
                    }

                    if (mEnergyPerServingCheckBox.isChecked() && mPortionText.equals("")) {
                        setErrorHints(mPortionTextView, getString(R.string.portionError));
                        wereErrors = true;
                    }

                    if (wereErrors & !Utilities.isInDebugMode()) return;

                    if (Utilities.isInDebugMode()) {
                        mBarcodeText = "072417136160";
                        mProductNameText = "Maryland Choc Chip";
                        mQuantityText = "230g";
                        mEnergyPerText = "450";
                        mIngredientsText = "Fortified wheat flour, Chocolate chips (25%), Sugar, Palm oil, Golden syrup, Whey and whey derivatives (Milk), Raising agents, Salt, Flavouring";
                        mTracesText = "Milk, Soya, Nuts, Wheat";
                        mWrittenTraces = ListHelper.stringToList(mTracesText);
                    }

                    List<String> ingredientsToDisplay = ListHelper.stringToList(mIngredientsText);

                    // Set values for passing back to scan fragment
                    mWrittenTraces = ListHelper.stringToList(mTracesText);

                    String ingredients = ListHelper.listToString(ingredientsToDisplay);

                    String user_id = getString(R.string.open_food_facts_username);
                    String password = getString(R.string.open_food_facts_password);

                    String uneditedProductName = mProductNameText;

                    try {
                        mProductNameText = URLEncoder.encode(mProductNameText, "UTF-8");
                        if (!mQuantityText.equals("")) {
                            mQuantityText = mQuantityText + mUnitText;
                        }
                        mQuantityText = URLEncoder.encode(mQuantityText, "UTF-8");
                        mEnergyPerText = URLEncoder.encode(mEnergyPerText, "UTF-8");
                        ingredients = URLEncoder.encode(ingredients, "UTF-8");
                        mTracesText = URLEncoder.encode(mTracesText.toLowerCase(), "UTF-8");
                        user_id = URLEncoder.encode(user_id, "UTF-8");
                        password = URLEncoder.encode(password, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e("ERROR", "Couldn't encode params properly");
                        e.printStackTrace();
                    }

                    mProductNameText = mProductNameText.replace("+", "%20");
                    ingredients = ingredients.toLowerCase();
                    ingredients = ingredients.replace("+", "%20");
                    ingredients = ingredients.replace("_", "%5F");

                    String params = "user_id=" + user_id +
                            "&password=" + password +
                            "&code=" + mBarcodeText + "&product_name=" + mProductNameText +
                            "&quantity=" + mQuantityText + "&nutriment_energy=" + mEnergyPerText +
                            "&nutriment_energy_unit=kJ&nutrition_data_per=";

                    if (mEnergyPerServingCheckBox.isChecked() && !mEnergyPerText.equals("")) {
                        params += "serving";
                    } else if (mEnergyPer100CheckBox.isChecked() && !mEnergyPerText.equals("")) {
                        params += "100g";
                    }

                    params += "&ingredients_text=" + ingredients + "&traces=" + mTracesText.toLowerCase();

                    try {
                        String url = BASE_URL + params;
                        submitData(url, progressBar, uneditedProductName, ingredientsToDisplay, mWrittenTraces, true, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Submits the product data to Open Food Facts. Retries every 30 seconds if no response (i.e. no connectivity).
     *
     * @param url                  API call with encoded parameters.
     * @param progressBar          Progress Bar used to show the user that there is something happening.
     *                             Only used first time round, as after that the user may want to continue
     *                             using the rest of the app.
     * @param name                 The name of the product.
     * @param ingredientsToDisplay The "cleaned" version of the ingredients which are shown to the user.
     *                             This should have any special characters removed e.g. _
     * @param writtenTraces        The list of traces the product has or may have.
     * @param shouldQueryData      If true, the data is also queried and displayed to the user. If false
     *                             it is usually because of a recursive call meaning we have already submitted
     *                             the data.
     * @param sleepAmounnt         The amount of time to wait between each retry.
     */
    private void submitData(final String url, final ProgressBar progressBar, final String name, final List<String> ingredientsToDisplay, final List<String> writtenTraces, final boolean shouldQueryData, final int sleepAmounnt) {
        QueryURLAsync rh = new QueryURLAsync(progressBar, sleepAmounnt, new QueryURLAsync.AsyncResponse() {
            @Override
            public void processFinish(String output) {
                // if response ok
                Log.d("processFinish", "output was: " + output);
                if (output != null) {
                    if (shouldQueryData) {
                        Log.d("processFinish", "querying data");
                        finishSubmitting(name, ingredientsToDisplay, writtenTraces);
                    } else {
                        Log.d("processFinish", "Not querying data, probably recursive call");
                    }
                } else {
                    finishSubmitting(name, ingredientsToDisplay, writtenTraces);
                    Log.d("submitData", "retrying queryUrlAsync");
                    submitData(url, null, name, ingredientsToDisplay, writtenTraces, false, 30000);
                }
                // else recursive call but don't do query again
            }
        });
        rh.execute(url);
    }

    /**
     * Converts parameters to a JSON object which is passed as a string. This is handled in onActivityResult
     * in ScanFragment and displayed to the user.
     *
     * @param name                 The name of the product.
     * @param ingredientsToDisplay The "cleaned" version of the ingredients which are shown to the user.
     *                             This should have any special characters removed e.g. _
     * @param writtenTraces        The list of traces the product has or may have.
     */
    private void finishSubmitting(String name, List<String> ingredientsToDisplay, List<String> writtenTraces) {
        Log.d("onDataChange", "Product submitted");
        JSONObject product = new JSONObject();
        try {
            product.put("product_name", name);
            product.put("ingredients_text", ListHelper.listToString(ingredientsToDisplay));
            product.put("traces", ListHelper.listToString(writtenTraces));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.putExtra("result", RESULT_OK);
        intent.putExtra("json", product.toString());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Sets a customised error message of a TextView.
     *
     * @param tv    The TextView to set the error message of.
     * @param error The error message.
     */
    private void setErrorHints(TextView tv, String error) {
        tv.setError(error);
    }

    /**
     * Sets a default error message of a TextView.
     *
     * @param tv The TextView to set the error message of.
     */
    private void setErrorHints(TextView tv) {
        tv.setError(getString(R.string.requiredField));
    }

}
