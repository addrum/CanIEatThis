package com.adamshort.canieatthis;

import android.app.Activity;
import android.util.Log;

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
        SetDatabasesFromFiles(activity);
    }

    public static DataQuerier getInstance(Activity activity) {
        if (mInstance == null) {
            mInstance = new DataQuerier(activity);
        }
        return mInstance;
    }

    public static boolean[] processData(List<String> ingredients, List<String> traces, boolean firebase, DataSnapshot snapshot) {
        boolean[] bools = new boolean[]{false, false, false, false,};
        ingredients = IngredientsList.RemoveUnwantedCharacters(ingredients, "[_]|\\s+$\"", "");
        traces = IngredientsList.RemoveUnwantedCharacters(traces, "[_]|\\s+$\"", "");
        if (firebase) {
            boolean lactose = true;
            boolean lacFalse = false;
            boolean vegetarian = true;
            boolean vegFalse = false;
            boolean vegan = true;
            boolean veganFalse = false;
            boolean gluten = true;
            boolean glutFalse = false;

            if (ingredients.size() > 0 && !ingredients.get(0).equals("")) {
                for (String resIngredient : ingredients) {
                    // replace any special characters as we need an exact match
                    String lowerResIngredient = DataQuerier.replaceSpecialChars(resIngredient).toLowerCase();
                    for (DataSnapshot ingredientSnapshot : snapshot.getChildren()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ing = (Map<String, Object>) ingredientSnapshot.getValue();
                        String name = ingredientSnapshot.getKey().toLowerCase();
                        if (name.equals(lowerResIngredient)) {
                            if (!lacFalse) {
                                boolean dai = (Boolean) ing.get("lactose_free");
                                if (!dai) {
                                    lactose = false;
                                    lacFalse = true;
                                }
                            }
                            if (!vegFalse) {
                                boolean veg = (Boolean) ing.get("vegetarian");
                                if (!veg) {
                                    vegetarian = false;
                                    vegFalse = true;
                                }
                            }
                            if (!veganFalse) {
                                boolean veg = (Boolean) ing.get("vegan");
                                if (!veg) {
                                    vegan = false;
                                    veganFalse = true;
                                }
                            }
                            if (!glutFalse) {
                                boolean glu = (Boolean) ing.get("gluten_free");
                                if (!glu) {
                                    gluten = false;
                                    glutFalse = true;
                                }
                            }
                            Log.d("onDataChange", name + " " + lactose + " " + vegetarian +
                                    " " + vegan + " " + gluten);
                        }
                    }
                }
            }

            if (traces.size() > 0 && !traces.get(0).equals("")) {
                if (!traces.get(0).equals("")) {
                    for (String trace : traces) {
                        if (!lacFalse) {
                            boolean d = IsLactoseFree(trace);
                            if (!d) {
                                lactose = false;
                                lacFalse = true;
                            }
                        }
                        if (!veganFalse) {
                            boolean v = IsVegan(trace);
                            if (!v) {
                                vegan = false;
                                veganFalse = true;
                            }
                        }
                        if (!vegFalse) {
                            boolean ve = IsVegetarian(trace);
                            if (!ve) {
                                vegetarian = false;
                                veganFalse = true;
                            }
                        }
                        if (glutFalse) {
                            boolean g = IsGlutenFree(trace);
                            if (!g) {
                                gluten = false;
                                glutFalse = true;
                            }
                        }
                    }
                }
            }
            bools[0] = lactose;
            bools[1] = vegetarian;
            bools[2] = vegan;
            bools[3] = gluten;
        } else {
            boolean lactose = IsLactoseFree(ingredients);
            boolean vegan = IsVegan(ingredients);
            boolean vegetarian = true;
            // if something is vegan it is 100% vegetarian
            if (!vegan) {
                vegetarian = IsVegetarian(ingredients);
            }
            boolean gluten = IsGlutenFree(ingredients);

            if (traces.size() > 0) {
                if (!traces.get(0).equals("")) {
                    for (String trace : traces) {
                        boolean d = IsLactoseFree(trace);
                        if (!d) {
                            lactose = false;
                        }
                        boolean v = IsVegan(trace);
                        if (!v) {
                            vegan = false;
                        }
                        boolean ve = IsVegetarian(trace);
                        if (!ve) {
                            vegetarian = false;
                        }
                        boolean g = IsGlutenFree(trace);
                        if (!g) {
                            gluten = false;
                        }
                    }
                }
            }
            bools[0] = lactose;
            bools[1] = vegetarian;
            bools[2] = vegan;
            bools[3] = gluten;
        }
        return bools;
    }

    public JSONObject ParseIntoJSON(String response) {
        try {
            return new JSONObject(response).getJSONObject("product");
        } catch (JSONException e) {
            Log.e("ParseIntoJSON", "Issue getting ingredients from URL, could be from csv: " + e);
        }
        return null;
    }

    public void SetDatabasesFromFiles(Activity activity) {
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

    public static boolean IsLactoseFree(List<String> list) {
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

    public static boolean IsLactoseFree(String ingredient) {
        for (String dairyIngredient : dairy) {
            if (ingredient.toLowerCase().equals(dairyIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public static boolean IsVegetarian(List<String> list) {
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

    public static boolean IsVegetarian(String ingredient) {
        for (String vegetarianIngredient : vegetarian) {
            if (ingredient.toLowerCase().equals(vegetarianIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public static boolean IsVegan(List<String> list) {
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

    public static boolean IsVegan(String ingredient) {
        for (String veganIngredient : vegan) {
            if (ingredient.toLowerCase().equals(veganIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public static boolean IsGlutenFree(List<String> list) {
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

    public static boolean IsGlutenFree(String ingredient) {
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
