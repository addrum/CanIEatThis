package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    static final int    DOWNLOAD = 0;
    static final int    PRODUCT = 1;
    static boolean DEBUG;

    public LinearLayout container;
    public RelativeLayout ingredientsLinearLayout;

    public TextView responseView;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the main content layout of the Activity
        setContentView(R.layout.activity_main);

        container = (LinearLayout) findViewById(R.id.container);
        ingredientsLinearLayout = (RelativeLayout) findViewById(R.id.ingredientsLinearLayout);

        responseView = (TextView) findViewById(R.id.responseView);
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

        DEBUG = android.os.Debug.isDebuggerConnected();
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
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                GetBarcodeInformation(contents);
            }
        }
    }

    public void GetBarcodeInformation(String barcode) {
        new RequestHandler().execute(BASE_URL + barcode + EXTENSION);
    }

    public void ParseIntoJSON(String response) {
        try {
            JSONObject object = new JSONObject(response);
            JSONObject product = object.getJSONObject("product");
            String ingredients = product.getString("ingredients_text");

            List<String> editedIngredients = StringToList(ingredients);

            boolean dairy = IsDairyFree(editedIngredients);
            boolean vegetarian = IsVegetarian(editedIngredients);
            boolean vegan = IsVegan(editedIngredients);
            boolean gluten = IsGlutenFree(editedIngredients);

            SetResponseTextBox(editedIngredients.toString());
            SetAllergenIcons(dairy, vegetarian, vegan, gluten);
        } catch (JSONException e) {
            Log.d("ERROR", "Issue getting ingredients from URL: " + e);

            showDialog(MainActivity.this, "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
        }
    }

    public List<String> StringToList(String s) {
        ArrayList<String> ingredients = new ArrayList<String>(Arrays.asList(s.split(", ")));
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

    public void SetAllergenIcons(boolean dairy, boolean vegetarian, boolean vegan, boolean gluten) {
        dairyFreeSwitch.setChecked(dairy);
        vegetarianSwitch.setChecked(vegetarian);
        veganSwitch.setChecked(vegan);
        glutenFreeSwitch.setChecked(gluten);
    }

    public void SetResponseTextBox(String response) {
        responseView.setText(response);
        ingredientsLinearLayout.setVisibility(View.VISIBLE);
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

            ParseIntoJSON(response);
        }
    }
}
