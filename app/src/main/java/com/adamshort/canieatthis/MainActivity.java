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
    public List<String> dairy = Arrays.asList("acidophilus milk",
            "ammonium caseinate",
            "butter",
            "butter fat",
            "butter oil",
            "butter solids",
            "buttermilk",
            "buttermilk powder",
            "calcium caseinate",
            "casein",
            "caseinate (in general)",
            "cheese (all animal-based)",
            "condensed milk",
            "cottage cheese",
            "cream",
            "curds",
            "custard",
            "delactosed whey",
            "demineralized whey",
            "dry milk powder",
            "dry milk solids",
            "evaporated milk",
            "ghee",
            "goat cheese",
            "goat milk",
            "half & half",
            "hydrolyzed casein",
            "hydrolyzed milk protein",
            "iron caseinate",
            "lactalbumin",
            "lactoferrin",
            "lactoglobulin",
            "lactose",
            "lactulose",
            "low-fat milk",
            "magnesium caseinate",
            "malted milk",
            "milk",
            "milk derivative",
            "milk fat",
            "milk powder",
            "milk protein",
            "milk solids",
            "natural butter flavor",
            "nonfat milk",
            "nougat",
            "paneer",
            "potassium caseinate",
            "pudding",
            "recaldent",
            "rennet casein",
            "sheep milk",
            "sheep milk cheese",
            "skim milk",
            "sodium caseinate",
            "sour cream",
            "sour milk solids",
            "sweetened condensed milk",
            "sweet whey",
            "whey",
            "whey powder",
            "whey protein concentrate",
            "whey protein hydrolysate",
            "whipped cream",
            "whipped topping",
            "whole milk",
            "yogurt",
            "zinc caseinate");

    public List<String> vegetarian = Arrays.asList();

    //http://www.peta.org/living/beauty/animal-ingredients-list/
    public List<String> vegan = Arrays.asList("adrenaline",
            "alanine",
            "albumen",
            "albumin",
            "alcloxa",
            "aldioxa",
            "aliphatic alcohol",
            "allantoin",
            "alligator skin",
            "alpha-hydroxy acids",
            "ambergris",
            "amino acids",
            "aminosuccinate acid",
            "angora",
            "animal fats and oils",
            "animal hair",
            "arachidonic acid",
            "arachidyl proprionate",
            "bee pollen",
            "bee products",
            "beeswax. honeycomb",
            "biotin. vitamin h. vitamin b factor",
            "blood",
            "boar bristles",
            "bone char",
            "bone meal",
            "calciferol",
            "calfskin",
            "caprylamine oxide",
            "capryl betaine",
            "caprylic acid",
            "caprylic triglyceride",
            "carbamide",
            "carmine.",
            "cochineal.",
            "carminic acid",
            "carminic acid",
            "carotene.",
            "provitamin a.",
            "beta carotene",
            "casein.",
            "caseinate.",
            "sodium caseinate",
            "caseinate",
            "cashmere",
            "castor. castoreum",
            "castoreum",
            "catgut",
            "cera flava",
            "cerebrosides",
            "cetyl alcohol",
            "cetyl palmitate",
            "chitosan",
            "cholesterin",
            "cholesterol",
            "choline bitartrate",
            "civet",
            "cochineal",
            "cod liver oil",
            "collagen",
            "colors. dyes",
            "corticosteroid",
            "cortisone",
            "corticosteroid",
            "cysteine, l-form",
            "cystine",
            "dexpanthenol",
            "diglycerides",
            "dimethyl stearamine",
            "down",
            "duodenum substances",
            "dyes",
            "egg protein",
            "elastin",
            "emu oil",
            "ergocalciferol",
            "ergosterol",
            "estradiol",
            "estrogen.",
            "estradiol",
            "fats",
            "fatty acids",
            "fd&c colors",
            "feathers",
            "fish liver oil",
            "fish oil",
            "fish scales",
            "fur",
            "gel",
            "gelatin. gel",
            "glucose tyrosinase",
            "glycerides",
            "glycerin. glycerol",
            "glycerol",
            "glyceryls",
            "glycreth-26",
            "guanine. pearl essence",
            "hide glue",
            "honey",
            "honeycomb",
            "horsehair",
            "hyaluronic acid",
            "hydrocortisone",
            "hydrolyzed animal protein",
            "imidazolidinyl urea",
            "insulin",
            "isinglass",
            "isopropyl lanolate",
            "isopropyl myristate",
            "isopropyl palmitate",
            "keratin",
            "lactic acid",
            "lactose",
            "laneth",
            "lanogene",
            "lanolin.",
            "lanolin acids.",
            "wool fat.",
            "wool wax",
            "lanolin alcohol",
            "lanosterols",
            "lard",
            "leather.",
            "suede.",
            "calfskin.",
            "sheepskin.",
            "alligator skin",
            "lecithin.",
            "choline bitartrate",
            "linoleic acid",
            "lipase",
            "lipids",
            "lipoids.",
            "lipids",
            "marine oil",
            "methionine",
            "milk protein",
            "mink oil",
            "monoglycerides.",
            "glycerides",
            "musk (oil)",
            "myristal ether sulfate",
            "myristic acid",
            "myristyls",
            "“natural sources.",
            "nucleic acids",
            "ocenol",
            "octyl dodecanol",
            "oleic acid",
            "oils",
            "oleths",
            "oleyl alcohol.",
            "ocenol",
            "oleyl arachidate",
            "oleyl imidazoline",
            "oleyl myristate",
            "oleyl oleate",
            "oleyl stearate",
            "palmitamide",
            "palmitamine",
            "palmitate",
            "palmitic acid",
            "panthenol.",
            "dexpanthenol.",
            "vitamin b-complex factor.",
            "provitamin b-5",
            "panthenyl",
            "pepsin",
            "placenta.",
            "placenta polypeptides protein.",
            "afterbirth",
            "polyglycerol",
            "polypeptides",
            "polysorbates",
            "pristane",
            "progesterone",
            "propolis",
            "provitamin a",
            "provitamin b-5",
            "provitamin d-2",
            "rennet. rennin",
            "rennin",
            "resinous glaze",
            "retinol",
            "ribonucleic acid",
            "rna. ribonucleic acid",
            "royal jelly",
            "sable brushes",
            "sea turtle oil",
            "shark liver oil",
            "sheepskin",
            "shellac. resinous glaze",
            "silk. silk powder",
            "snails",
            "sodium caseinate",
            "sodium steroyl lactylate",
            "sodium tallowate",
            "spermaceti. cetyl palmitate.",
            "sperm oil",
            "sponge (luna and sea)",
            "squalane",
            "squalene",
            "stearamide",
            "stearamine",
            "stearamine oxide",
            "stearates",
            "stearic acid",
            "stearic hydrazide",
            "stearone",
            "stearoxytrimethylsilane",
            "stearoyl lactylic acid",
            "stearyl acetate",
            "stearyl alcohol. sterols",
            "stearyl betaine",
            "stearyl caprylate",
            "stearyl citrate",
            "stearyldimethyl amine",
            "stearyl glycyrrhetinate",
            "stearyl heptanoate",
            "stearyl imidazoline",
            "stearyl octanoate",
            "stearyl stearate",
            "steroids. sterols",
            "sterols",
            "suede",
            "tallow. tallow fatty alcohol.",
            "stearic acid",
            "tallow acid",
            "tallow amide",
            "tallow amine",
            "talloweth-6",
            "tallow glycerides",
            "tallow imidazoline",
            "triterpene alcohols",
            "turtle oil. sea turtle oil",
            "tyrosine",
            "urea. carbamide",
            "uric acid",
            "vitamin a",
            "vitamin b-complex factor",
            "vitamin b factor",
            "vitamin b",
            "vitamin d.",
            "ergocalciferol.",
            "vitamin d.",
            "ergosterol.",
            "provitamin d.",
            "calciferol.",
            "vitamin d",
            "vitamin h",
            "wax",
            "whey",
            "wool",
            "wool fat",
            "wool wax");

    public List<String> gluten = Arrays.asList("abyssinian hard (wheat triticum durum)",
            "alcohol (spirits - specific types)",
            "atta flour",
            "barley grass (can contain seeds)",
            "barley hordeum vulgare",
            "barley malt",
            "beer (most contain barley or wheat)",
            "bleached flour ",
            "bran",
            "bread flour",
            "brewer's yeast",
            "brown flour",
            "bulgur (bulgar wheat/nuts) ",
            "bulgur wheat",
            "cereal binding",
            "chilton",
            "club wheat (triticum aestivum subspecies compactum) ",
            "common wheat (triticum aestivum)",
            "cookie crumbs",
            "cookie dough",
            "cookie dough pieces",
            "couscous",
            "criped rice",
            "dinkle (spelt)",
            "disodium wheatgermamido peg-2 sulfosuccinate ",
            "durum wheat (triticum durum)",
            "edible coatings",
            "edible films",
            "edible starch",
            "einkorn (triticum monococcum)",
            "emmer (triticum dicoccon) ",
            "enriched bleached flour",
            "enriched bleached wheat flour",
            "enriched flour",
            "farina ",
            "farina graham ",
            "farro",
            "filler",
            "flour (normally this is wheat)",
            "fu (dried wheat gluten)",
            "germ ",
            "graham flour",
            "granary flour",
            "groats (barley, wheat) ",
            "hard wheat",
            "heeng",
            "hing",
            "hordeum vulgare extract",
            "hydroxypropyltrimonium hydrolyzed wheat protein ",
            "kamut (pasta wheat) ",
            "kecap manis (soy sauce)",
            "ketjap manis (soy sauce)",
            "kluski pasta",
            "maida (indian wheat flour)",
            "malt",
            "malted barley flour",
            "malted milk",
            "malt extract",
            "malt syrup",
            "malt flavoring",
            "malt vinegar ",
            "macha wheat (triticum aestivum) ",
            "matza",
            "matzah",
            "matzo",
            "matzo semolina ",
            "meripro 711",
            "mir ",
            "nishasta",
            "oriental wheat (triticum turanicum) ",
            "orzo pasta",
            "pasta",
            "pearl barley",
            "persian wheat (triticum carthlicum) ",
            "perungayam",
            "poulard wheat (triticum turgidum)",
            "polish wheat (triticum polonicum) ",
            "rice malt (if barley or koji are used)",
            "roux",
            "rusk",
            "rye",
            "seitan",
            "semolina",
            "semolina triticum",
            "shot wheat (triticum aestivum) ",
            "small spelt",
            "spirits (specific types)",
            "spelt (triticum spelta)",
            "sprouted wheat or barley",
            "stearyldimoniumhydroxypropyl hydrolyzed wheat protein ",
            "strong flour",
            "suet in packets",
            "tabbouleh ",
            "tabouli",
            "teriyaki sauce",
            "timopheevi wheat (triticum timopheevii) ",
            "triticale x triticosecale",
            "triticum vulgare (wheat) flour lipids",
            "triticum vulgare (wheat) germ extract",
            "triticum vulgare (wheat) germ oil",
            "udon (wheat noodles)",
            "unbleached flour ",
            "vavilovi wheat (triticum aestivum) ",
            "vital wheat gluten",
            "wheat, abyssinian hard triticum durum",
            "wheat amino acids",
            "wheat bran extract",
            "wheat, bulgur ",
            "wheat durum triticum ",
            "wheat germ extract",
            "wheat germ glycerides",
            "wheat germ oil",
            "wheat germamidopropyldimonium hydroxypropyl hydrolyzed wheat protein",
            "wheat grass (can contain seeds) ",
            "wheat nuts",
            "wheat protein",
            "wheat triticum aestivum ",
            "wheat triticum monococcum",
            "wheat (triticum vulgare) bran extract",
            "whole-meal flour",
            "wild einkorn (triticum boeotictim) ",
            "wild emmer (triticum dicoccoides)");

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
            boolean vegetarian = IsVegetarian(editedIngredients);
            boolean vegan = IsVegan(editedIngredients);
            boolean gluten = IsGlutenFree(editedIngredients);

            SetResponseTextBox(editedIngredients.toString());
            SetAllergenIcons(dairy, vegetarian, vegan, gluten);
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
