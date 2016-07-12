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
    private static DataQuerier mInstance = null;
    public static List<String> traces;

    private DataQuerier(Activity activity) {
        setTracesFromFile(activity);
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

        int ingSize = ingredients.size();
        int traSize = traces.size();
        if ((ingSize == 0 || ingredients.get(0).equals("")) && (traSize == 0 || traces.get(0).equals(""))) {
            return new boolean[]{false, false, false, false};
        }

        Boolean[] bools = new Boolean[]{null, null, null, null};

        bools = checkIfValuesAreSuitable(ingredients, snapshot, bools);
        bools = checkIfValuesAreSuitable(traces, snapshot, bools);

        return new boolean[]{bools[0] != null && bools[0], bools[1] != null && bools[1],
                bools[2] != null && bools[2], bools[3] != null && bools[3]};
    }

    private static Boolean[] checkIfValuesAreSuitable(List<String> values, DataSnapshot snapshot, Boolean[] bools) {
        if (values.size() > 0 && !values.get(0).equals("")) {
            for (String value : values) {
                String lowerResValue = replaceSpecialChars(value.toLowerCase());
                for (DataSnapshot ingredientSnapshot : snapshot.getChildren()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ing = (Map<String, Object>) ingredientSnapshot.getValue();
                    String name = ingredientSnapshot.getKey().toLowerCase();
                    if (name.equals(lowerResValue)) {
                        if (bools[0] == null || bools[0]) {
                            bools[0] = (Boolean) ing.get("lactose_free");
                        }
                        if (bools[1] == null || bools[1]) {
                            bools[1] = (Boolean) ing.get("vegetarian");
                        }
                        if (bools[2] == null || bools[2]) {
                            bools[2] = (Boolean) ing.get("vegan");
                        }
                        if (bools[3] == null || bools[3]) {
                            bools[3] = (Boolean) ing.get("gluten_free");
                        }
                        Log.d("processDataFirebase", name + " lactose_free: " + bools[0] + " vegetarian: " + bools[1] +
                                " vegan: " + bools[2] + " gluten_free: " + bools[3]);
                    }
                }
            }
        }
        return bools;
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

    public static JSONObject parseIntoJSON(String response) {
        try {
            return new JSONObject(response).getJSONObject("product");
        } catch (JSONException e) {
            Log.e("parseIntoJSON", "issue getting ingredients from URL, could be from csv: " + e);
        }
        return null;
    }

    public static void setTracesFromFile(Activity activity) {
        traces = new ArrayList<>();

        BufferedReader reader;

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

    public static String replaceSpecialChars(String s) {
        s = s.replaceAll("\\([^)]*\\)", "")
                .replace("_", "")
                .trim();
        return s;
    }

    public static List<String> getTraces() {
        return traces;
    }

}
