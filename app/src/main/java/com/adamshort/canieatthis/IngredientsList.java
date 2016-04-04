package com.adamshort.canieatthis;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IngredientsList {

    public static List<String> StringToList(String s) {
        return new ArrayList<>(Arrays.asList(s.split(", ")));
    }

    public static String ListToString(List<String> s) {
        StringBuilder sb = new StringBuilder();
        for (String st : s) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(st);
        }
        return sb.toString();
    }

    public static List<String> RemoveUnwantedCharacters(List<String> ingredients, String regex, String replaceWith) {
        for (int i = 0; i < ingredients.size(); i++) {
            ingredients.set(i, ingredients.get(i).replaceAll(regex, replaceWith));
            Log.i("INFO", ingredients.get(i));
        }
        return ingredients;
    }

}
