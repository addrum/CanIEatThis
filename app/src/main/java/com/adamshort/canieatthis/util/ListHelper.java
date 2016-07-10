package com.adamshort.canieatthis.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListHelper {

    public static List<String> stringToList(String s) {
        return new ArrayList<>(Arrays.asList(trimArray(s.split(","))));
    }

    public static List<String> stringToListAndTrim(String s) {
        // strip percentages in brackets
        s = s.replaceAll("\\(\\d*%\\d*\\)", "");
        // strip ))
        s = s.replaceAll("\\)\\),", ",");
        // replace all ( or )
        s = s.replaceAll("\\(|\\),|\\)", ", ");
        // replace : or ; with ,
        s = s.replaceAll(":|;", ",");
        // split "Or" into two
        s = s.replaceAll("(?i)\\sor\\s", ",");
        // split "And" into two
        s = s.replaceAll("(?i)\\sand\\s", ",");
        // replace - with ,
        s = s.replaceAll("-", ",");
        String [] array = s.split(",");
        return new ArrayList<>(Arrays.asList(trimArray(array)));
    }

    private static String[] trimArray(String[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
        return array;
    }

    public static String listToString(List<String> s) {
        StringBuilder sb = new StringBuilder();
        for (String st : s) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(st);
        }
        return sb.toString();
    }

    public static List<String> removeUnwantedCharacters(List<String> ingredients, String regex, String replaceWith) {
        for (int i = 0; i < ingredients.size(); i++) {
            ingredients.set(i, ingredients.get(i).replaceAll(regex, replaceWith));
            Log.i("UnwantedCharacters", ingredients.get(i));
        }
        return ingredients;
    }

}
