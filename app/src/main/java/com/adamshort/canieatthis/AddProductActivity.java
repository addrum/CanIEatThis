package com.adamshort.canieatthis;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class AddProductActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://world.openfoodfacts.org/cgi/product_jqm2.pl?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
    }
}
