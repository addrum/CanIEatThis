package com.adamshort.canieatthis.ui.activity;

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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.adamshort.canieatthis.data.DataPasser;
import com.adamshort.canieatthis.data.DataQuerier;
import com.adamshort.canieatthis.util.IngredientsList;
import com.adamshort.canieatthis.util.QueryURLAsync;
import com.adamshort.canieatthis.R;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.apache.commons.lang3.text.WordUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.adamshort.canieatthis.data.DataQuerier.*;

public class AddProductActivity extends AppCompatActivity {

    public static boolean DEBUG;

    private static final String BASE_URL = "http://world.openfoodfacts.org/cgi/product_jqm2.pl?";
    private static String barcode = "";

    private String barcodeText;
    private String productNameText;
    private String quantityText;
    private String unitText;
    private String energyPerText;
    private String ingredientsText;
    private String tracesText;
    private String itemTitle;
    private String portionText;
    private List<String> writtenIngredients, writtenTraces;

    private TextView barcodeNumberTextView;
    private TextView productNameTextView;
    private TextView quantityTextView;
    private TextView energyPerTextView;
    private TextView ingredientsTextView;
    private TextView tracesTextView;
    private CheckBox energyPerServingCheckBox;
    private CheckBox energyPer100CheckBox;
    private TextView portionTextView;

    private QueryURLAsync rh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        DEBUG = android.os.Debug.isDebuggerConnected();

        Bundle b = getIntent().getExtras();
        if (b != null) {
            barcode = b.getString("barcode");
        }

        barcodeNumberTextView = (TextView) findViewById(R.id.input_barcode_number);
        productNameTextView = (TextView) findViewById(R.id.input_product_name);
        quantityTextView = (TextView) findViewById(R.id.input_quantity);
        energyPerTextView = (TextView) findViewById(R.id.input_energy_per);
        ingredientsTextView = (TextView) findViewById(R.id.input_ingredients);
        tracesTextView = (TextView) findViewById(R.id.input_traces);
        energyPerServingCheckBox = (CheckBox) findViewById(R.id.input_energy_per_serving);
        energyPer100CheckBox = (CheckBox) findViewById(R.id.input_energy_per_100g);
        portionTextView = (TextView) findViewById(R.id.input_portion);

        energyPerServingCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (energyPer100CheckBox.isChecked()) {
                    energyPer100CheckBox.setChecked(false);
                }
                energyPerTextView.setError(null);
                portionTextView.setEnabled(!portionTextView.isEnabled());
            }
        });
        energyPer100CheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (energyPerServingCheckBox.isChecked()) {
                    energyPerServingCheckBox.setChecked(false);
                }
                energyPerTextView.setError(null);
                portionTextView.setEnabled(false);
                portionTextView.setError(null);
            }
        });

        if (productNameTextView.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        final Spinner unitSpinner = (Spinner) findViewById(R.id.input_unit);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.unitValues, android.R.layout.simple_spinner_dropdown_item);
        if (unitSpinner != null) {
            unitSpinner.setAdapter(adapter);
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        Button submitProductButton = (Button) findViewById(R.id.product_submit_button);

        barcodeNumberTextView.setText(barcode);

        if (submitProductButton != null) {
            submitProductButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    barcodeText = barcodeNumberTextView.getText().toString();
                    productNameText = productNameTextView.getText().toString();
                    quantityText = quantityTextView.getText().toString();
                    if (unitSpinner != null) {
                        unitText = unitSpinner.getSelectedItem().toString();
                    }
                    energyPerText = energyPerTextView.getText().toString();
                    ingredientsText = ingredientsTextView.getText().toString();
                    tracesText = tracesTextView.getText().toString();
                    portionText = portionTextView.getText().toString();

                    List<TextView> required = new ArrayList<>();
                    required.add(barcodeNumberTextView);
                    required.add(productNameTextView);
                    required.add(ingredientsTextView);

                    boolean wereErrors = false;

                    for (TextView req : required) {
                        if (req.getText().toString().isEmpty()) {
                            setErrorHints(req);
                            wereErrors = true;
                        }
                    }

                    if (!energyPerText.equals("") && (!energyPerServingCheckBox.isChecked() &&
                            !energyPer100CheckBox.isChecked())) {
                        setErrorHints(energyPerTextView, getString(R.string.noEnergyCheckedError));
                        wereErrors = true;
                    }

                    if (energyPerText.equals("") && (energyPerServingCheckBox.isChecked() ||
                            energyPer100CheckBox.isChecked())) {
                        setErrorHints(energyPerTextView, getString(R.string.energyError));
                    }

                    if (energyPerServingCheckBox.isChecked() && portionText.equals("")) {
                        setErrorHints(portionTextView, getString(R.string.portionError));
                        wereErrors = true;
                    }

                    if (wereErrors & !DEBUG) return;

                    final List<String> ingredientsToTest = IngredientsList.stringToListAndTrim(ingredientsText);
                    List<String> ingredientsToDisplay = IngredientsList.stringToList(ingredientsText);

                    // Set values for passing back to scan fragment
                    itemTitle = productNameText;
                    writtenIngredients = ingredientsToDisplay;
                    writtenTraces = IngredientsList.stringToList(tracesText);

                    List<String> traces = DataQuerier.getInstance(AddProductActivity.this).getTraces();

                    String ingredients = IngredientsList.listToString(compareTwoLists(ingredientsToDisplay, traces));

                    if (DEBUG) {
                        barcodeText = "072417136160";
                        productNameText = "Maryland Choc Chip";
                        itemTitle = productNameText;
                        quantityText = "230g";
                        energyPerText = "450";
                        ingredients = "Fortified wheat flour, Chocolate chips (25%), Sugar, Palm oil, Golden syrup, Whey and whey derivatives (Milk), Raising agents, Salt, Flavouring";
                        writtenIngredients = IngredientsList.stringToList(ingredients);
                        tracesText = "Milk, Soya, Nuts, Wheat";
                        writtenTraces = IngredientsList.stringToList(tracesText);
                    }

                    String user_id = getString(R.string.open_food_facts_username);
                    String password = getString(R.string.open_food_facts_password);

                    try {
                        productNameText = URLEncoder.encode(productNameText, "UTF-8");
                        if (!quantityText.equals("")) {
                            quantityText = quantityText + unitText;
                        }
                        quantityText = URLEncoder.encode(quantityText, "UTF-8");
                        energyPerText = URLEncoder.encode(energyPerText, "UTF-8");
                        ingredients = URLEncoder.encode(ingredients, "UTF-8");
                        tracesText = URLEncoder.encode(tracesText, "UTF-8");
                        user_id = URLEncoder.encode(user_id, "UTF-8");
                        password = URLEncoder.encode(password, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e("ERROR", "Couldn't encode params properly");
                        e.printStackTrace();
                    }

                    productNameText = productNameText.replace("+", "%20");
                    ingredients = ingredients.replace("+", "%20");
                    ingredients = ingredients.replace("_", "%5F");

                    String params = "user_id=" + user_id +
                            "&password=" + password +
                            "&code=" + barcodeText + "&product_name=" + productNameText +
                            "&quantity=" + quantityText + "&nutriment_energy=" + energyPerText +
                            "&nutriment_energy_unit=kJ&nutrition_data_per=";

                    if (energyPerServingCheckBox.isChecked() && !energyPerText.equals("")) {
                        params += "serving";
                    } else if (energyPer100CheckBox.isChecked() && !energyPerText.equals("")) {
                        params += "100g";
                    }

                    params += "&ingredients_text=" + ingredients + "&traces=" + tracesText;

                    try {
                        String url = BASE_URL + params;
                        Log.d("onCreate", "Url to execute at is: " + url);
                        rh = new QueryURLAsync(AddProductActivity.this, progressBar, new QueryURLAsync.AsyncResponse() {
                            @Override
                            public void processFinish(String output) {
                                Firebase ref = new Firebase(getString(R.string.firebase_url) + "/ingredients");
                                ref.keepSynced(true);
                                ref.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        boolean[] bools = processDataFirebase(ingredientsToTest, writtenTraces, snapshot);

                                        setDataPasser(bools[0], bools[1], bools[2], bools[3]);

                                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                        intent.putExtra("result", RESULT_OK);
                                        setResult(Activity.RESULT_OK, intent);
                                        finish();
                                    }

                                    @Override
                                    public void onCancelled(FirebaseError error) {
                                    }
                                });
                            }
                        });
                        rh.execute(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    }

    private List<String> compareTwoLists(List<String> list1, List<String> list2) {
        for (int i = 0; i < list1.size(); i++) {
            String ing = list1.get(i).toLowerCase();
            for (int j = 0; j < list2.size(); j++) {
                if (ing.contains(list2.get(j))) {
                    list1.set(i, WordUtils.capitalize(ing.replace(list2.get(j), "_" + list2.get(j) + "_")));
                }
            }
        }
        return list1;
    }

    private void setDataPasser(boolean dairy, boolean vegetarian, boolean vegan, boolean gluten) {
        DataPasser.getInstance().setQuery(itemTitle);

        DataPasser.getInstance().setDairy(dairy);
        DataPasser.getInstance().setVegetarian(vegetarian);
        DataPasser.getInstance().setVegan(vegan);
        DataPasser.getInstance().setGluten(gluten);

        DataPasser.getInstance().setSwitchesVisible(true);
        DataPasser.getInstance().setItemVisible(true);
        DataPasser.getInstance().setIntroVisible(false);
        DataPasser.getInstance().setResponseVisible(true);

        DataPasser.getInstance().setFromSearch(false);

        DataPasser.getInstance().setIngredients(IngredientsList.listToString(writtenIngredients));
        DataPasser.getInstance().setTraces(IngredientsList.listToString(writtenTraces));
    }

    private void setErrorHints(TextView tv, String error) {
        tv.setError(error);
    }

    private void setErrorHints(TextView tv) {
        tv.setError(getString(R.string.requiredField));
    }

}
