package com.adamshort.canieatthis.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PreferencesHelper {

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

}
