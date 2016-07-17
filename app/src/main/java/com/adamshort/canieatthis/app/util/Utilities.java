package com.adamshort.canieatthis.app.util;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
        long lastPref = PreferencesHelper.getTimestampPref(context);
        if (lastPref == 0) {
            // Pref is probably empty when app is first installed so set it to current time as default
            PreferencesHelper.setTimestampPref(context, System.currentTimeMillis());
        }
        Timestamp last = new Timestamp(lastPref);

        Log.d("isTimeForUpdatePrompt", "Current: " + current.getTime() + " - Last: " + last.getTime());

        String frequency = PreferencesHelper.getFrequencyListPref(context);
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
