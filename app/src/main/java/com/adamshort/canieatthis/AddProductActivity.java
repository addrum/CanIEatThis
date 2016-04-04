package com.adamshort.canieatthis;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddProductActivity extends AppCompatActivity {

    public static boolean DEBUG;

    private static final String BASE_URL = "http://world.openfoodfacts.org/cgi/product_jqm2.pl?";
    private static final String END_ERROR_MSG = "Required field.";
    private static String barcode = "";

    private String barcodeText, productNameText, quantityText, energyPerServingText, ingredientsText, tracesText;

    private TextView barcodeNumberTextView;
    private TextView productNameTextView;
    private TextView quantityTextView;
    private TextView energyPerServingTextView;
    private TextView ingredientsTextView;
    private TextView tracesTextView;

    private List<String> traces = Arrays.asList("peanuts",
            "nuts",
            "almonds",
            "hazelnuts",
            "walnuts",
            "brazil nuts",
            "cashews",
            "pecans",
            "pistachios",
            "macadamia nuts",
            "queensland nut",
            "eggs",
            "milk",
            "crustaceans",
            "prawns",
            "crabs",
            "lobsters",
            "fish",
            "sesame seeds",
            "cereals",
            "gluten",
            "wheat",
            "rye",
            "barley",
            "oats",
            "soya",
            "celery",
            "mustard",
            "sulphur dioxide",
            "sulphites",
            "lupin",
            "molluscs");

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

        Button submitProductButton = (Button) findViewById(R.id.product_submit_button);

        barcodeNumberTextView.setText(barcode);

        final List<String> debugIngredients = Arrays.asList("Fortified Wheat Flour", "Sugar", "Palm Oil", "Hydrolysed Wheat Gluten", "Soya Lecithin");

        submitProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeText = barcodeNumberTextView.getText().toString();
                productNameText = productNameTextView.getText().toString();
                quantityText = quantityTextView.getText().toString();
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

                if (wereErrors) return;

                List<String> editedIngredients = IngredientsList.StringToList(ingredientsText);

                if (DEBUG) {
                    editedIngredients = debugIngredients;
                }

                for (int i = 0; i < editedIngredients.size(); i++) {
                    String ing = editedIngredients.get(i).toLowerCase();
                    for (int j = 0; j < traces.size(); j++) {
                        if (ing.contains(traces.get(j))) {
                            editedIngredients.set(i, WordUtils.capitalize(ing.replace(traces.get(j), "_" + traces.get(j) + "_")));
                        }
                    }
                }

                String ingredients = IngredientsList.ListToString(editedIngredients);

                String params = "code=" + barcodeText + "&product_name=" + productNameText + "&quantity=" + quantityText + "&nutriment_energy=" + energyPerServingText + "&nutriment_energy_unit=kJ&nutrition_data_per=serving" +
                        "&ingredients_text=" + ingredients + "&traces=" + tracesText;

                params = params.replace(" ", "%20");
                params = params.replace(",", "%2C");

                try {
                    String response = new RequestHandler().execute((BASE_URL + params)).get();
                } catch (Exception e) {
                    Log.e("ERROR", "Couldn't get a response");
                    e.printStackTrace();
                }

            }
        });
    }

    private void SetErrorHints(TextView tv) {
       tv.setError(END_ERROR_MSG);
    }

}
