package com.adamshort.canieatthis.util;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.adamshort.canieatthis.R;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public class Utilities {

    private static Utilities mInstance = null;
    private static DownloadManager downloadManager;
    private static FileDownloader fileDownloader;

    private Utilities() {
    }

    public static Utilities getInstance() {
        if (mInstance == null) {
            mInstance = new Utilities();
        }
        return mInstance;
    }

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
        boolean intro_shown = preferences.getBoolean("frequency_list_pref", false);
        Log.d("getFrequencyListPref", "frequency_list_pref: " + intro_shown);
        return preferences.getString("frequency_list_pref", "0");
    }

    public static void setFrequencyListPref(Context context, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("frequency_list_pref", value);
        editor.apply();
        Log.d("setFrequencyListPref", "frequency_list_pref: " + value);
    }

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static void downloadDatabase(Activity activity, Context context) {
        if (activity != null && context != null) {
            downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            fileDownloader = FileDownloader.getInstance(activity, downloadManager,
                    context.getString(R.string.csvURL), "products.csv.tmp");

            // Update timestamp since we've downloaded a new one
            SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("timestamp", System.currentTimeMillis());
            editor.apply();
        }
    }

    public static boolean timeForUpdatePrompt(Context context, Timestamp current) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastPref = prefs.getLong("timestamp", current.getTime());
        if (lastPref == 0) {
            // Pref is probably empty when app is first installed so set it to current time as default
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("timestamp", System.currentTimeMillis());
            editor.apply();
        }
        Timestamp last = new Timestamp(lastPref);

        Log.d("timeForUpdatePrompt", "Current: " + current.getTime() + " - Last: " + last.getTime());

        String frequency = prefs.getString("frequency_list_pref", "0");
        Log.d("timeForUpdatePrompt", "Frequency is " + frequency);

        long days = TimeUnit.MILLISECONDS.toDays(current.getTime() - last.getTime());
        Log.d("timeForUpdatePrompt", "Days is " + days);

        switch (frequency) {
            case "0":
                if (days > 1) {
                    return true;
                }
                break;
            case "1":
                if (days > 7) {
                    return true;
                }
                break;
            case "2":
                if (days > 28) {
                    return true;
                }
                break;
        }
        return false;
    }

    public static DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public static FileDownloader getFileDownloader() {
        return fileDownloader;
    }

}
