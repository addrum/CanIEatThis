package com.adamshort.canieatthis.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PreferencesHelper {

    public static boolean getIntroShownPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean intro_shown = preferences.getBoolean("intro_shown", false);
        Log.d("getIntroShownPref", "intro_shown: " + intro_shown);
        return preferences.getBoolean("intro_shown", false);
    }

    public static void setIntroShownPref(Context context, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("intro_shown", value);
        editor.apply();
        Log.d("setIntroShownPref", "intro_shown: " + value);
    }

    public static boolean getDownloadSwitchPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean intro_shown = preferences.getBoolean("download_switch_pref", false);
        Log.d("getDownloadSwitchPref", "download_switch_pref: " + intro_shown);
        return preferences.getBoolean("download_switch_pref", false);
    }

    public static void setDownloadSwitchPref(Context context, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("download_switch_pref", value);
        editor.apply();
        Log.d("setDownloadSwitchPref", "download_switch_pref: " + value);
    }

    public static String getFrequencyListPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String frequency_list_pref = preferences.getString("frequency_list_pref", "0");
        Log.d("getFrequencyListPref", "frequency_list_pref: " + frequency_list_pref);
        return preferences.getString("frequency_list_pref", "0");
    }

    public static void setFrequencyListPref(Context context, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("frequency_list_pref", value);
        editor.apply();
        Log.d("setFrequencyListPref", "frequency_list_pref: " + value);
    }

    public static boolean getLactoseFreePref(Context context) {
        if (context != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            return preferences.getBoolean("lactose_free_pref", false);
        }
        Log.d("getLactoseFreePref", "Context was null. Defaulting for false");
        return false;
    }

    public static void setLactoseFreePref(Context context, boolean lactose) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("lactose_free_pref", lactose);
        editor.apply();
        Log.d("setLactoseFreePref", "lactose_free_pref: " + lactose);
    }

    public static boolean getVegetarianPref(Context context) {
        if (context != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            return preferences.getBoolean("vegetarian_pref", false);
        }
        Log.d("getLactoseFreePref", "Context was null. Defaulting for false");
        return false;
    }

    public static void setVegetarianPref(Context context, boolean vegetarian) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("vegetarian_pref", vegetarian);
        editor.apply();
        Log.d("setVegetarianPref", "vegetarian_pref: " + vegetarian);
    }

    public static boolean getVeganPref(Context context) {
        if (context != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            return preferences.getBoolean("vegan_pref", false);
        }
        Log.d("getLactoseFreePref", "Context was null. Defaulting for false");
        return false;
    }

    public static void setVeganPref(Context context, boolean vegan) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("vegan_pref", vegan);
        editor.apply();
        Log.d("setVeganPref", "vegan_pref " + vegan);
    }

    public static boolean getGlutenFreePref(Context context) {
        if (context != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            return preferences.getBoolean("gluten_free_pref", false);
        }
        Log.d("getLactoseFreePref", "Context was null. Defaulting for false");
        return false;
    }

    public static void setGlutenFreePref(Context context, boolean glutenFree) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("gluten_free_pref", glutenFree);
        editor.apply();
        Log.d("setGlutenFreePref", "gluten_free_pref " + glutenFree);
    }

    public static boolean getInstallationFileDeletedPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean frequency_list_pref = preferences.getBoolean("installation_file_deleted_pref", false);
        Log.d("getInstallationFileDel", "installation_file_deleted_pref: " + frequency_list_pref);
        return frequency_list_pref;
    }

    public static void setInstallationFileDeletedPref(Context context, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("installation_file_deleted_pref", value);
        editor.apply();
        Log.d("setInstallationFileDel", "installation_file_deleted_pref " + value);
    }

    public static boolean getFromSearchPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean frequency_list_pref = preferences.getBoolean("from_search", false);
        Log.d("getFromSearchPref", "from_search: " + frequency_list_pref);
        return frequency_list_pref;
    }

    public static void setFromSearchPref(Context context, boolean fromSearch) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("from_search", fromSearch);
        editor.apply();
        Log.d("setFromSearchPref", "from_search " + fromSearch);
    }

    public static int getTimesAskedForPermPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int times_asked = preferences.getInt("times_asked", 0);
        Log.d("getTimesAskedForPerm", "times_asked: " + times_asked);
        return preferences.getInt("times_asked", 0);
    }

    public static void setTimesAskedForPermPref(Context context, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("times_asked", value);
        editor.apply();
        Log.d("setTimesAskedForPerm", "times_asked: " + value);
    }

    public static int getTimesAskedForCameraPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int times_asked = preferences.getInt("times_asked_camera", 0);
        Log.d("getTimesAskedForCamera", "times_asked_camera: " + times_asked);
        return preferences.getInt("times_asked_camera", 0);
    }

    public static void setTimesAskedForCameraPref(Context context, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("times_asked_camera", value);
        editor.apply();
        Log.d("setTimesAskedForCamera", "times_asked_camera: " + value);
    }

    public static long getTimestampPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long timestamp = preferences.getLong("timestamp", 0);
        Log.d("getTimestampPref", "timestamp: " + timestamp);
        return preferences.getLong("timestamp", 0);
    }

    public static void setTimestampPref(Context context, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("timestamp", value);
        editor.apply();
        Log.d("setTimestampPref", "timestamp: " + value);
    }
}
