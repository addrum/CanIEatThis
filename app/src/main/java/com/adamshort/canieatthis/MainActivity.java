package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    static final String BASE_URL = "http://world.openfoodfacts.org/api/v0/product/";
    static final String EXTENSION = ".json";
    static final int DOWNLOAD = 0;
    static final int PRODUCT = 1;
    static boolean DEBUG;

    public LinearLayout container;
    public RelativeLayout responseLinearLayout;

    public TextView itemTextView;
    public TextView ingredientResponseView;
    public TextView tracesResponseView;
    public Button scanButton;

    public Switch dairyFreeSwitch;
    public Switch vegetarianSwitch;
    public Switch veganSwitch;
    public Switch glutenFreeSwitch;

    //http://www.godairyfree.org/dairy-free-grocery-shopping-guide/dairy-ingredient-list-2
    public List<String> dairy;
    public List<String> vegetarian;
    //http://www.peta.org/living/beauty/animal-ingredients-list/
    public List<String> vegan;
    public List<String> gluten;

    private Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the main content layout of the Activity
        setContentView(R.layout.activity_main);

        container = (LinearLayout) findViewById(R.id.container);
        responseLinearLayout = (RelativeLayout) findViewById(R.id.responseLinearLayout);

        itemTextView = (TextView) findViewById(R.id.itemTitleText);
        ingredientResponseView = (TextView) findViewById(R.id.ingredientsResponseView);
        tracesResponseView = (TextView) findViewById(R.id.tracesResponseView);
        scanButton = (Button) findViewById(R.id.scanButton);

        dairyFreeSwitch = (Switch) findViewById(R.id.dairyFreeSwitch);
        vegetarianSwitch = (Switch) findViewById(R.id.vegetarianSwitch);
        veganSwitch = (Switch) findViewById(R.id.veganSwitch);
        glutenFreeSwitch = (Switch) findViewById(R.id.glutenFreeSwitch);

        dairyFreeSwitch.setClickable(false);
        vegetarianSwitch.setClickable(false);
        veganSwitch.setClickable(false);
        glutenFreeSwitch.setClickable(false);

        SetDatabasesFromFiles();

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.findItem(R.id.action_search).collapseActionView();
            }
        });

        DEBUG = android.os.Debug.isDebuggerConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Menu actionMenu = menu;
        this.menu = menu;
        getMenuInflater().inflate(R.menu.main, actionMenu);

        // Define the listener
        MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when action item collapses
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;  // Return true to expand action view
            }
        };

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) actionMenu.findItem(R.id.action_search).getActionView();
        if (null != searchView) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered
                return true;
            }

            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    // query is the value entered into the search bar
                    boolean dairy = IsDairyFree(query);
                    boolean vegetarian = IsVegetarian(query);
                    boolean vegan = IsVegan(query);
                    boolean gluten = IsGlutenFree(query);
                    SetAllergenIcons(dairy, vegetarian, vegan, gluten);
                    actionMenu.findItem(R.id.action_search).collapseActionView();
                    responseLinearLayout.setVisibility(View.INVISIBLE);
                    itemTextView.setText(String.format(getString(R.string.ingredient), query));
                }
                return true;
            }
        };

        if (searchView != null) {
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean queryTextFocused) {
                    if (!queryTextFocused) {
                        actionMenu.findItem(R.id.action_search).collapseActionView();
                        searchView.setQuery("", false);
                    }
                }
            });

            searchView.setOnQueryTextListener(queryTextListener);
        }


        // Assign the listener to that action item
        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.action_search), expandListener);

        return super.onCreateOptionsMenu(menu);
    }

    //product barcode mode
    public void scanBar(View v) {
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
            showDialog(MainActivity.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No", DOWNLOAD).show();
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
            if (resultCode == RESULT_OK) {
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
        JSONObject product = ParseIntoJSON(response);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> editedIngredients = StringToList(ingredients);
                List<String> editedTraces = StringToList(traces);

                boolean dairy = IsDairyFree(editedIngredients);
                boolean vegetarian = IsVegetarian(editedIngredients);
                boolean vegan = IsVegan(editedIngredients);
                boolean gluten = IsGlutenFree(editedIngredients);

                SetItemTitleText(item);
                SetAllergenIcons(dairy, vegetarian, vegan, gluten);
                SetIngredientsResponseTextBox(editedIngredients.toString());
                SetTracesResponseTextBox(editedTraces.toString());
                responseLinearLayout.setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            Log.d("ERROR", "Issue ParseIntoJSON(response)");
        }
    }

    public JSONObject ParseIntoJSON(String response) {
        try {
            JSONObject object = new JSONObject(response);
            return object.getJSONObject("product");
        } catch (JSONException e) {
            Log.d("ERROR", "Issue getting ingredients from URL: " + e);

            showDialog(MainActivity.this, "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
        }
        return null;
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

    public boolean IsDairyFree(List<String> list) {
        for (String ingredient : list) {
            for (String dairyIngredient : dairy) {
                if (ingredient.toLowerCase().contains(dairyIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsDairyFree(String ingredient) {
        for (String dairyIngredient : dairy) {
            if (ingredient.toLowerCase().contains(dairyIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public boolean IsVegetarian(List<String> list) {
        for (String ingredient : list) {
            for (String vegetarianIngredient : vegetarian) {
                if (ingredient.toLowerCase().contains(vegetarianIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsVegetarian(String ingredient) {
        for (String vegetarianIngredient : vegetarian) {
            if (ingredient.toLowerCase().contains(vegetarianIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public boolean IsVegan(List<String> list) {
        for (String ingredient : list) {
            for (String veganIngredient : vegan) {
                if (ingredient.toLowerCase().contains(veganIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsVegan(String ingredient) {
        for (String veganIngredient : vegan) {
            if (ingredient.toLowerCase().contains(veganIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public boolean IsGlutenFree(List<String> list) {
        for (String ingredient : list) {
            for (String glutenIngredient : gluten) {
                if (ingredient.toLowerCase().contains(glutenIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsGlutenFree(String ingredient) {
        for (String glutenIngredient : gluten) {
            if (ingredient.toLowerCase().contains(glutenIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public void SetDatabasesFromFiles() {
        dairy = new ArrayList<>();
        vegetarian = new ArrayList<>();
        vegan = new ArrayList<>();
        gluten = new ArrayList<>();

        BufferedReader reader;

        try {
            final InputStream file = getAssets().open("dairy.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                Log.d("Dairy", line);
                line = reader.readLine();
                if (line != null) {
                    dairy.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            final InputStream file = getAssets().open("vegetarian.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                Log.d("Vegetarian", line);
                line = reader.readLine();
                if (line != null) {
                    vegetarian.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            final InputStream file = getAssets().open("vegan.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                Log.d("Vegan", line);
                line = reader.readLine();
                if (line != null) {
                    vegan.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            final InputStream file = getAssets().open("gluten.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                Log.d("Gluten", line);
                line = reader.readLine();
                if (line != null) {
                    gluten.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void SetItemTitleText(String item) {
        itemTextView.setText(String.format(getString(R.string.product), item));
        itemTextView.setVisibility(View.VISIBLE);
    }

    public void SetAllergenIcons(boolean dairy, boolean vegetarian, boolean vegan, boolean gluten) {
        dairyFreeSwitch.setChecked(dairy);
        vegetarianSwitch.setChecked(vegetarian);
        veganSwitch.setChecked(vegan);
        glutenFreeSwitch.setChecked(gluten);
    }

    public void SetIngredientsResponseTextBox(String response) {
        ingredientResponseView.setText(response);
    }

    public void SetTracesResponseTextBox(String response) {
        tracesResponseView.setText(response);
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
