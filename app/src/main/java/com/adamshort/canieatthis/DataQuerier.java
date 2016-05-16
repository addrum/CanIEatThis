package com.adamshort.canieatthis;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DataQuerier {

    //http://www.godairyfree.org/dairy-free-grocery-shopping-guide/dairy-ingredient-list-2
    public List<String> dairy;
    public List<String> vegetarian;
    //http://www.peta.org/living/beauty/animal-ingredients-list/
    public List<String> vegan;
    public List<String> gluten;
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

    public boolean IsLactoseFree(List<String> list) {
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

    public boolean IsLactoseFree(String ingredient) {
        for (String dairyIngredient : dairy) {
            if (ingredient.toLowerCase().equals(dairyIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public boolean IsVegetarian(List<String> list) {
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

    public boolean IsVegetarian(String ingredient) {
        for (String vegetarianIngredient : vegetarian) {
            if (ingredient.toLowerCase().equals(vegetarianIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public boolean IsVegan(List<String> list) {
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

    public boolean IsVegan(String ingredient) {
        for (String veganIngredient : vegan) {
            if (ingredient.toLowerCase().equals(veganIngredient.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public boolean IsGlutenFree(List<String> list) {
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

    public boolean IsGlutenFree(String ingredient) {
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
