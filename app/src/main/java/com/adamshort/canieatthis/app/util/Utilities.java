package com.adamshort.canieatthis.app.util;

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
    private static DownloadManager mDownloadManager;
    private static FileDownloader mFileDownloader;
    private static Utilities mInstance = null;

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

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static void downloadDatabase(Activity activity, Context context) {
        if (activity != null && context != null) {
            mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            mFileDownloader = FileDownloader.getInstance(activity, mDownloadManager,
                    context.getString(R.string.csvURL), "products.csv.tmp");

            // Update timestamp since we've downloaded a new one
            SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("timestamp", System.currentTimeMillis());
            editor.apply();
        }
    }

    public static boolean isTimeForUpdatePrompt(Context context, Timestamp current) {
        long lastPref = getTimestampPref(context);
        if (lastPref == 0) {
            // Pref is probably empty when app is first installed so set it to current time as default
            setTimestampPref(context, System.currentTimeMillis());
        }
        Timestamp last = new Timestamp(lastPref);

        Log.d("isTimeForUpdatePrompt", "Current: " + current.getTime() + " - Last: " + last.getTime());

        String frequency = getFrequencyListPref(context);
        Log.d("isTimeForUpdatePrompt", "Frequency is " + frequency);

        long days = TimeUnit.MILLISECONDS.toDays(current.getTime() - last.getTime());
        Log.d("isTimeForUpdatePrompt", "Days is " + days);

        switch (frequency) {
            case "0":
                if (days > 0) {
                    Log.d("isTimeForUpdatePrompt", "More than a day has passed, showing prompt");
                    return true;
                }
                break;
            case "1":
                if (days > 6) {
                    Log.d("isTimeForUpdatePrompt", "More than a week has passed, showing prompt");
                    return true;
                }
                break;
            case "2":
                if (days > 27) {
                    Log.d("isTimeForUpdatePrompt", "More than a month has passed, showing prompt");
                    return true;
                }
                break;
        }
        Log.d("isTimeForUpdatePrompt", "Not enough time has past, not showing prompt");
        return false;
    }

    public static DownloadManager getDownloadManager() {
        return mDownloadManager;
    }

    public static FileDownloader getFileDownloader() {
        return mFileDownloader;
    }

    public static boolean isPortraitMode(Context context) {
        boolean portrait = context.getResources().getBoolean(R.bool.portrait_only);
        Log.d("isPortraitMode", "portrait only: " + portrait);
        return portrait;
    }

    public static boolean isInDebugMode() {
        return android.os.Debug.isDebuggerConnected();
    }

}
