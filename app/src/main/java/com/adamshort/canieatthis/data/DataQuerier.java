package com.adamshort.canieatthis.data;

import android.app.Activity;
import android.util.Log;

import com.adamshort.canieatthis.util.ListHelper;
import com.firebase.client.DataSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataQuerier {

    //http://www.godairyfree.org/dairy-free-grocery-shopping-guide/dairy-ingredient-list-2
    public static List<String> dairy;
    public static List<String> vegetarian;
    //http://www.peta.org/living/beauty/animal-ingredients-list/
    public static List<String> vegan;
    public static List<String> gluten;
    public List<String> traces;

    private static DataQuerier mInstance = null;

    private DataQuerier(Activity activity) {
        setDatabasesFromFiles(activity);
    }

    public static DataQuerier getInstance(Activity activity) {
        if (mInstance == null) {
            mInstance = new DataQuerier(activity);
        }
        return mInstance;
    }

    public static boolean[] processDataFirebase(List<String> ingredients, List<String> traces, DataSnapshot snapshot) {
        Log.d("processDataFirebase", "Processing data with firebase");
        ingredients = ListHelper.removeUnwantedCharacters(ingredients, "[_]|\\s+$\"", "");
        traces = ListHelper.removeUnwantedCharacters(traces, "[_]|\\s+$\"", "");

        Boolean lactose = null;
        Boolean vegetarian = null;
        Boolean vegan = null;
        Boolean gluten = null;

        int ingSize = ingredients.size();
        int traSize = traces.size();
        if ((ingSize == 0 || ingredients.get(0).equals("")) && (traSize == 0 || traces.get(0).equals(""))) {
            return new boolean[]{false, false, false, false};
        }

        if (ingSize > 0 && !ingredients.get(0).equals("")) {
            for (String resIngredient : ingredients) {
                // replace any special characters as we need an exact match
                String lowerResIngredient = DataQuerier.replaceSpecialChars(resIngredient).toLowerCase();
                for (DataSnapshot ingredientSnapshot : snapshot.getChildren()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ing = (Map<String, Object>) ingredientSnapshot.getValue();
                    String name = ingredientSnapshot.getKey().toLowerCase();
                    if (name.equals(lowerResIngredient)) {
                        if (lactose == null || lactose) {
                            lactose = (Boolean) ing.get("lactose_free");
                        }
                        if (vegetarian == null || vegetarian) {
                            vegetarian = (Boolean) ing.get("vegetarian");
                        }
                        if (vegan == null || vegan) {
                            vegan = (Boolean) ing.get("vegan");
                        }
                        if (gluten == null || gluten) {
                            gluten = (Boolean) ing.get("gluten_free");
                        }
                        Log.d("processDataFirebase", name + " " + lactose + " " + vegetarian +
                                " " + vegan + " " + gluten);
                    }
                }
            }
        }

        if (traSize > 0 && !traces.get(0).equals("")) {
            if (!traces.get(0).equals("")) {
                for (String trace : traces) {
                    if (lactose == null || lactose) {
                        lactose = isLactoseFree(trace);
                    }
                    if (vegetarian == null || vegetarian) {
                        vegetarian = isVegetarian(trace);
                    }
                    if (vegan == null || vegan) {
                        vegan = isVegan(trace);
                    }
                    if (gluten == null || gluten) {
                        gluten = isGlutenFree(trace);
                    }
                }
            }
        }
        return new boolean[]{lactose != null && lactose, vegetarian != null && vegetarian,
                vegan != null && vegan, gluten != null && gluten};
    }

    public static boolean[] processData(List<String> ingredients, List<String> traces) {
        Log.d("processData", "Processing data with text files");
        Boolean lactose = isLactoseFree(ingredients);
        Boolean vegan = isVegan(ingredients);
        Boolean vegetarian = null;
        // if something is vegan it is 100% vegetarian
        if (!vegan) {
            vegetarian = isVegetarian(ingredients);
        }
        Boolean gluten = isGlutenFree(ingredients);

        if (traces.size() > 0) {
            if (!traces.get(0).equals("")) {
                for (String trace : traces) {
                    if (lactose) {
                        lactose = isLactoseFree(trace);
                    }
                    if (vegetarian == null || vegetarian) {
                        vegetarian = isVegetarian(trace);
                    }
                    if (vegan) {
                        vegan = isVegan(trace);
                    }
                    if (gluten) {
                        gluten = isGlutenFree(trace);
                    }
                }
            }
        }

        return new boolean[]{lactose, vegetarian != null && vegetarian,
                vegan, gluten};
    }

    public static boolean[] processIngredientFirebase(String ingredient, DataSnapshot snapshot) {
        Log.d("processDataFirebase", "Processing ingredient with firebase");
        Boolean lactose = null;
        Boolean vegetarian = null;
        Boolean vegan = null;
        Boolean gluten = null;

        for (DataSnapshot ingredientSnapshot : snapshot.getChildren()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ing = (Map<String, Object>) ingredientSnapshot.getValue();
            String name = ingredientSnapshot.getKey().toLowerCase();
            if (name.equals(ingredient)) {
                if (lactose == null || lactose) {
                    lactose = (Boolean) ing.get("lactose_free");
                }
                if (vegetarian == null || vegetarian) {
                    vegetarian = (Boolean) ing.get("vegetarian");
                }
                if (vegan == null || vegan) {
                    vegan = (Boolean) ing.get("vegan");
                }
                if (gluten == null || gluten) {
                    gluten = (Boolean) ing.get("gluten_free");
                }
                Log.d("processIngFirebase", name + " " + lactose + " " + vegetarian +
                        " " + vegan + " " + gluten);
            }
        }

        Log.d("processIngFirebase", ingredient + " " + lactose + " " + vegetarian +
                " " + vegan + " " + gluten);

        return new boolean[]{lactose != null && lactose, vegetarian != null && vegetarian,
                vegan != null && vegan, gluten != null && gluten};
    }

    public static boolean[] processIngredient(String ingredient) {
        Log.d("processData", "Processing ingredient with text files");
        boolean lactose = isLactoseFree(ingredient);
        boolean vegan = isVegan(ingredient);
        boolean vegetarian = false;
        // if something is vegan it is 100% vegetarian
        if (!vegan) {
            vegetarian = isVegetarian(ingredient);
        }
        boolean gluten = isGlutenFree(ingredient);

        Log.d("processIngredient", ingredient + " " + lactose + " " + vegetarian +
                " " + vegan + " " + gluten);

        return new boolean[]{lactose, vegan, vegetarian, gluten};
    }

    public JSONObject parseIntoJSON(String response) {
        try {
            return new JSONObject(response).getJSONObject("product");
        } catch (JSONException e) {
            Log.e("parseIntoJSON", "issue getting ingredients from URL, could be from csv: " + e);
        }
        return null;
    }

    public void setDatabasesFromFiles(Activity activity) {
        dairy = new ArrayList<>();
        vegetarian = new ArrayList<>();
        vegan = new ArrayList<>();
        gluten = new ArrayList<>();
        traces = new ArrayList<>();

        BufferedReader reader;

        try {
            final InputStream file = activity.getAssets().open("dairy.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (line != null) {
                    dairy.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            final InputStream file = activity.getAssets().open("vegetarian.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (line != null) {
                    vegetarian.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            final InputStream file = activity.getAssets().open("vegan.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (line != null) {
                    vegan.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            final InputStream file = activity.getAssets().open("gluten.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (line != null) {
                    gluten.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            final InputStream file = activity.getAssets().open("traces.txt");
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                if (line != null) {
                    traces.add(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static boolean isLactoseFree(List<String> list) {
        for (String ingredient : list) {
            ingredient = replaceSpecialChars(ingredient);
            for (String dairyIngredient : dairy) {
                if (ingredient.toLowerCase().equals(dairyIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isLactoseFree(String ingredient) {
        for (String lactoseIngredient : dairy) {
                if (ingredient.toLowerCase().equals(lactoseIngredient.toLowerCase())) {
                    return false;
                }
        }
        return true;
    }

    public static boolean isVegetarian(List<String> list) {
        for (String ingredient : list) {
            ingredient = replaceSpecialChars(ingredient);
            for (String vegetarianIngredient : vegetarian) {
                if (ingredient.toLowerCase().equals(vegetarianIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isVegetarian(String ingredient) {
        for (String vegetarianIngredient : vegetarian) {
            if (ingredient.toLowerCase().equals(vegetarianIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isVegan(List<String> list) {
        for (String ingredient : list) {
            ingredient = replaceSpecialChars(ingredient);
            for (String veganIngredient : vegan) {
                if (ingredient.toLowerCase().equals(veganIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isVegan(String ingredient) {
        for (String veganIngredient : vegan) {
            if (ingredient.toLowerCase().equals(veganIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isGlutenFree(List<String> list) {
        for (String ingredient : list) {
            ingredient = replaceSpecialChars(ingredient);
            for (String glutenIngredient : gluten) {
                if (ingredient.toLowerCase().equals(glutenIngredient.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isGlutenFree(String ingredient) {
        for (String glutenIngredient : gluten) {
            if (ingredient.toLowerCase().equals(glutenIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public static String replaceSpecialChars(String s) {
        s = s.replaceAll("\\([^)]*\\)", "")
                .replace("_", "")
                .trim();
        return s;
    }

    public List<String> getTraces() {
        return traces;
    }

}
