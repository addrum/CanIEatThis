package com.adamshort.canieatthis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.text.WordUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class AddProductActivity extends Activity {

    public static boolean DEBUG;

    private static final String BASE_URL = "http://world.openfoodfacts.org/cgi/product_jqm2.pl?";
    private static final String END_ERROR_MSG = "Required field.";
    private static String barcode = "";

    private String barcodeText;
    private String productNameText;
    private String quantityText;
    private String unitText;
    private String energyPerServingText;
    private String ingredientsText;
    private String tracesText;
    private String itemTitle;

    private TextView barcodeNumberTextView;
    private TextView productNameTextView;
    private TextView quantityTextView;
    private TextView energyPerServingTextView;
    private TextView ingredientsTextView;
    private TextView tracesTextView;

    private List<String> writtenIngredients, writtenTraces;

    private DataQuerier dataQuerier;

    private RequestHandler rh;

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
        energyPerServingTextView = (TextView) findViewById(R.id.input_energy_per_serving);
        ingredientsTextView = (TextView) findViewById(R.id.input_ingredients);
        tracesTextView = (TextView) findViewById(R.id.input_traces);

        if(productNameTextView.requestFocus() && !barcodeNumberTextView.getText().equals("")) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        final Spinner unitSpinner = (Spinner) findViewById(R.id.input_unit);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.unitValues, android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Button submitProductButton = (Button) findViewById(R.id.product_submit_button);

        barcodeNumberTextView.setText(barcode);

        dataQuerier = DataQuerier.getInstance(this);

        rh = new RequestHandler(this.getBaseContext(), progressBar, new RequestHandler.AsyncResponse() {
            @Override
            public void processFinish(String output) {

                writtenIngredients = IngredientsList.RemoveUnwantedCharacters(writtenIngredients, "[_]|\\s+$\"", "");

                writtenTraces = IngredientsList.RemoveUnwantedCharacters(writtenTraces, "[_]|\\s+$\"", "");

                boolean dairy = dataQuerier.IsDairyFree(writtenIngredients);
                boolean vegan = dataQuerier.IsVegan(writtenIngredients);
                boolean vegetarian = true;
                // if something is vegan it is 100% vegetarian
                if (!vegan) {
                    vegetarian = dataQuerier.IsVegetarian(writtenIngredients);
                }
                boolean gluten = dataQuerier.IsGlutenFree(writtenIngredients);

                if (writtenTraces.size() > 0) {
                    if (!writtenTraces.get(0).equals("")) {
                        for (String trace : writtenTraces) {
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

                DataPasser.getInstance().setIngredients(IngredientsList.ListToString(writtenIngredients));
                DataPasser.getInstance().setTraces(IngredientsList.ListToString(writtenTraces));

                Toast.makeText(getBaseContext(), "Product posted successfully", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        submitProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeText = barcodeNumberTextView.getText().toString();
                productNameText = productNameTextView.getText().toString();
                quantityText = quantityTextView.getText().toString();
                unitText = unitSpinner.getSelectedItem().toString();
                energyPerServingText = energyPerServingTextView.getText().toString();
                ingredientsText = ingredientsTextView.getText().toString();
                tracesText = tracesTextView.getText().toString();

                List<TextView> required = new ArrayList<>();
                required.add(barcodeNumberTextView);
                required.add(productNameTextView);
                required.add(ingredientsTextView);

                boolean wereErrors = false;

                for (TextView req : required) {
                    if (req.getText().toString().isEmpty()) {
                        SetErrorHints(req);
                        wereErrors = true;
                    }
                }

                if (wereErrors && !DEBUG) return;

                List<String> editedIngredients = IngredientsList.StringToList(ingredientsText);

                // Set values for passing back to scan fragment
                itemTitle = productNameText;
                writtenIngredients = editedIngredients;
                writtenTraces = IngredientsList.StringToList(tracesText);

                List<String> traces = DataQuerier.getInstance(AddProductActivity.this).getTraces();

                for (int i = 0; i < editedIngredients.size(); i++) {
                    String ing = editedIngredients.get(i).toLowerCase();
                    for (int j = 0; j < traces.size(); j++) {
                        if (ing.contains(traces.get(j))) {
                            editedIngredients.set(i, WordUtils.capitalize(ing.replace(traces.get(j), "_" + traces.get(j) + "_")));
                        }
                    }
                }

                String ingredients = IngredientsList.ListToString(editedIngredients);

//                if (DEBUG) {
//                    barcodeText = "072417136160";
//                    productNameText = "Maryland Choc Chip";
//                    itemTitle = productNameText;
//                    quantityText = "230g";
//                    energyPerServingText = "450";
//                    ingredients = "Fortified wheat flour, Chocolate chips (25%), Sugar, Palm oil, Golden syrup, Whey and whey derivatives (Milk), Raising agents, Salt, Flavouring";
//                    writtenIngredients = IngredientsList.StringToList(ingredients);
//                    tracesText = "Milk, Soya, Nuts, Wheat";
//                    writtenTraces = IngredientsList.StringToList(tracesText);
//                }

                String user_id = getString(R.string.open_food_facts_username);
                String password = getString(R.string.open_food_facts_password);

                try {
                    productNameText = URLEncoder.encode(productNameText, "UTF-8");
                    quantityText = URLEncoder.encode(quantityText + unitText, "UTF-8");
                    energyPerServingText = URLEncoder.encode(energyPerServingText, "UTF-8");
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
                        "&quantity=" + quantityText + "&nutriment_energy=" + energyPerServingText +
                        "&nutriment_energy_unit=kJ&nutrition_data_per=serving" +
                        "&ingredients_text=" + ingredients + "&traces=" + tracesText;

                try {
                    String url = BASE_URL + params;
                    Log.d("onCreate", "Url to execute at is: " + url);
                    rh.execute(url);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void SetErrorHints(TextView tv) {
       tv.setError(END_ERROR_MSG);
    }

}
