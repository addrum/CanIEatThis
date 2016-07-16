package com.adamshort.canieatthis.app.data;

import android.util.Log;

import com.adamshort.canieatthis.app.util.ListHelper;
import com.firebase.client.DataSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class DataQuerier {
    private static DataQuerier mInstance = null;

    private DataQuerier() {
    }

    public static DataQuerier getInstance() {
        if (mInstance == null) {
            mInstance = new DataQuerier();
        }
        return mInstance;
    }

    /**
     * Processes data using data contained in firebase, checking if it's suitable for each dietary requirement.
     * Removes unwanted characters from both lists. Uses a firebase snapshot which holds the data.
     * 0 = lactose_free
     * 1 = vegetarian
     * 2 = vegan
     * 3 = gluten_free
     *
     * @param ingredients List of ingredients to check.
     * @param traces      List of traces to check.
     * @param snapshot    A firebase snapshot.
     * @return Array of size 4, containing true or false for each dietary requirement.
     */
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

    /**
     * Checks if the values in the list are suitable for each dietary requirement.
     * 0 = lactose_free
     * 1 = vegetarian
     * 2 = vegan
     * 3 = gluten_free
     *
     * @param values   The list which is looped over.
     * @param snapshot A firebase snapshot which is also looped over for comparison to @values.
     * @param bools    First method call should pass array of nulls. Second method call should use the return value
     *                 of the first method call.
     * @return Array of size 4, containing true or false for each dietary requirement.
     */
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
                        Log.d("checkIfValsAreSuitable", name + " lactose_free: " + bools[0] + " vegetarian: " + bools[1] +
                                " vegan: " + bools[2] + " gluten_free: " + bools[3]);
                    }
                }
            }
        }
        return bools;
    }

    /**
     * Checks if the singular ingredient is suitable for each dietary requirement.
     * 0 = lactose_free
     * 1 = vegetarian
     * 2 = vegan
     * 3 = gluten_free
     *
     * @param ingredient A singular ingredient to test.
     * @param snapshot   A firebase snapshot which is also looped over for comparison to @ingredient.
     * @return Array of size 4, containing true or false for each dietary requirement.
     */
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

    /**
     * Converts the response returned by Open Food Facts to JSON.
     *
     * @param response The response returned by GET request to Open Food Facts.
     * @return Only the "product" value of the response.
     */
    public static JSONObject parseIntoJSON(String response) {
        try {
            return new JSONObject(response).getJSONObject("product");
        } catch (JSONException e) {
            Log.e("parseIntoJSON", "issue getting ingredients from URL, could be from csv: " + e);
        }
        return null;
    }

    /**
     * Removes any characters matching the regex. Used for testing a "cleaner" version of an ingredient.
     *
     * @param s The string to remove characters from.
     * @return The string with removed characters.
     */
    public static String replaceSpecialChars(String s) {
        s = s.replaceAll("\\([^)]*\\)", "")
                .replace("_", "")
                .trim();
        return s;
    }

}
