package com.adamshort.canieatthis.util;

import android.util.Log;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListHelper {

    /**
     * Converts a string to a list using , as the delimiter. Also trims the list.
     *
     * @param s
     */
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
        String[] array = s.split(",");
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

    /**
     * Replaces an element in a list with a string based on regex param
     *
     * @param ingredients the list to loop through
     * @param regex       the regex used to match an element
     * @param replaceWith the string to replace the matched regex
     */
    public static List<String> removeUnwantedCharacters(List<String> ingredients, String regex, String replaceWith) {
        for (int i = 0; i < ingredients.size(); i++) {
            ingredients.set(i, ingredients.get(i).replaceAll(regex, replaceWith));
            Log.i("UnwantedCharacters", ingredients.get(i));
        }
        return ingredients;
    }

    /**
     * Compares two lists, prepending and append a _ to an element if it's contained in both lists
     *
     * @param list1 this list is returned with modifications (if any)
     * @param list2 this is the list to compare to
     */
    public static List<String> compareTwoLists(List<String> list1, List<String> list2) {
        for (int i = 0; i < list1.size(); i++) {
            String ing = list1.get(i).toLowerCase();
            for (int j = 0; j < list2.size(); j++) {
                if (ing.contains(list2.get(j))) {
                    if (!ing.startsWith("_") && !ing.endsWith("_")) {
                        list1.set(i, WordUtils.capitalize(ing.replace(list2.get(j), "_" + list2.get(j) + "_")));
                    }
                }
            }
        }
        return list1;
    }
}
