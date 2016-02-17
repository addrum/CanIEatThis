package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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

    public LinearLayout container;
    public TextView responseView;
    public Button scanButton;
    public CheckBox dairyCheckBox;

    public List<String> dairy = Arrays.asList("Acidophilus Milk",
            "Ammonium Caseinate",
            "Butter",
            "Butter Fat",
            "Butter Oil",
            "Butter Solids",
            "Buttermilk",
            "Buttermilk Powder",
            "Calcium Caseinate",
            "Casein",
            "Caseinate (in general)",
            "Cheese (All animal-based)",
            "Condensed Milk",
            "Cottage Cheese",
            "Cream",
            "Curds",
            "Custard",
            "Delactosed Whey",
            "Demineralized Whey",
            "Dry Milk Powder",
            "Dry Milk Solids",
            "Evaporated Milk",
            "Ghee (see page 109 in Go Dairy Free)",
            "Goat Cheese",
            "Goat Milk",
            "Half & Half",
            "Hydrolyzed Casein",
            "Hydrolyzed Milk Protein",
            "Iron Caseinate",
            "Lactalbumin",
            "Lactoferrin",
            "Lactoglobulin",
            "Lactose",
            "Lactulose",
            "Low-Fat Milk",
            "Magnesium Caseinate",
            "Malted Milk",
            "Milk",
            "Milk Derivative",
            "Milk Fat",
            "Milk Powder",
            "Milk Protein",
            "Milk Solids",
            "Natural Butter Flavor",
            "Nonfat Milk",
            "Nougat",
            "Paneer",
            "Potassium Caseinate",
            "Pudding",
            "Recaldent",
            "Rennet Casein",
            "Sheep Milk",
            "Sheep Milk Cheese",
            "Skim Milk",
            "Sodium Caseinate",
            "Sour Cream",
            "Sour Milk Solids",
            "Sweetened Condensed Milk",
            "Sweet Whey",
            "Whey",
            "Whey Powder",
            "Whey Protein Concentrate",
            "Whey Protein Hydrolysate",
            "Whipped Cream",
            "Whipped Topping",
            "Whole Milk",
            "Yogurt",
            "Zinc Caseinate");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the main content layout of the Activity
        setContentView(R.layout.activity_main);

        container = (LinearLayout) findViewById(R.id.container);
        responseView = (TextView) findViewById(R.id.responseView);
        scanButton = (Button) findViewById(R.id.scanButton);
        dairyCheckBox = (CheckBox) findViewById(R.id.dairyCheckBox);
    }

    //product barcode mode
    public void scanBar(View v) {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(MainActivity.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
        }
    }

    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
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
        return downloadDialog.show();
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Toast toast = Toast.makeText(this, "Content:" + contents + " Format:" + format, Toast.LENGTH_LONG);
                toast.show();
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
            //SetResponseTextBox(editedIngredients);
            SetAllergenIcons(dairy, false, false, false);
        } catch (JSONException e) {
            Log.d("ERROR", "Issue getting ingredients from URL: " + e);
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
        String i = ingredient.replaceAll("[_%()]|(?<=\\().*?(?=\\))", "");
        // replace any whitespace with nothing
        return i.replaceAll("\\s+$", "");
    }

    public boolean IsDairyFree(List<String> list) {
        for (String ingredient : list) {
            if (dairy.contains(ingredient)) {
                return true;
            }
        }
        return false;
    }

    public void SetAllergenIcons(boolean dairy, boolean vegetarian, boolean vegan, boolean gluten) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout iconLayout = new RelativeLayout(this);
        iconLayout.setLayoutParams(params);
        container.addView(iconLayout);

        if (dairy) {
            ImageView dairyView = new ImageView(this);
            dairyView.setScaleType(ImageView.ScaleType.MATRIX);
            dairyView.setImageResource(R.drawable.dairy_free_icon);
            iconLayout.addView(dairyView);
        }
    }

    public void SetResponseTextBox(String response) {
        responseView.setText(response);
        responseView.setVisibility(View.VISIBLE);
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
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if(response == null) {
                Log.d("ERROR", "THERE WAS AN ERROR");
                return;
            }
            Log.i("INFO", response);

            ParseIntoJSON(response);
        }
    }
}
