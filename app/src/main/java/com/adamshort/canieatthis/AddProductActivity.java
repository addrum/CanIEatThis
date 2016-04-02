package com.adamshort.canieatthis;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddProductActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://world.openfoodfacts.org/cgi/product_jqm2.pl?";

    private String barcodeText, productNameText, quantityText, energyPerServingText, ingredientsText;

    private Button submitProductButton;
    private TextView barcodeNumberTextView;
    private TextView productNameTextView;
    private TextView quantityTextView;
    private TextView energyPerServingTextView;
    private TextView ingredientsTextView;

    private List<String> traces = Arrays.asList("peanuts",
            "nuts",
            "almonds",
            "hazelnuts",
            "walnuts",
            "Brazil nuts",
            "cashews",
            "pecans",
            "pistachios",
            "macadamia nuts",
            "Queensland nut",
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


        barcodeNumberTextView = (TextView) findViewById(R.id.input_barcode_number);
        productNameTextView = (TextView) findViewById(R.id.input_product_name);
        quantityTextView = (TextView) findViewById(R.id.input_quantity);
        energyPerServingTextView = (TextView) findViewById(R.id.input_energy_per_serving);
        ingredientsTextView = (TextView) findViewById(R.id.input_ingredients);

        submitProductButton = (Button) findViewById(R.id.product_submit_button);

        submitProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeText = barcodeNumberTextView.getText().toString();
                productNameText = productNameTextView.getText().toString();
                quantityText = quantityTextView.getText().toString();
                energyPerServingText = energyPerServingTextView.getText().toString();
                ingredientsText = ingredientsTextView.getText().toString();

                StringBuilder sb;
                List<String> editedIngredients = IngredientsList.StringToList(ingredientsText);
                for (int i = 0; i < editedIngredients.size(); i++) {
                    String ing = editedIngredients.get(i);
                    if (traces.contains(ing)) {
                        sb = new StringBuilder(ing);
                        sb.insert(0, "_");
                        sb.append("_");
                        editedIngredients.add(i, sb.toString());
                    }
                }

                String ingredients = IngredientsList.ListToString(editedIngredients);

                String params = "code=" + barcodeText + "&product_name=" + productNameText + "&quantity=" + quantityText + "&nutriment_energy=" + energyPerServingText + "&nutriment_energy_unit=kJ&nutrition_data_per=serving" +
                        "&ingredients_text=" + ingredients;

//                String response = new RequestHandler().GetResponse(BASE_URL + params);

            }
        });
    }


}
