package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class FileDownloader {

    private long downloadReference;

    public FileDownloader(Activity activity, DownloadManager downloadManager, String url) {
        Log.d("DEBUG", "Download file at: " + url);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDescription("Local database for CanIEatThis");
        request.setTitle("CanIEatThis Database");
        request.setDestinationInExternalPublicDir(Environment.getExternalStorageDirectory().getPath(), "products.csv");
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);

        downloadReference = downloadManager.enqueue(request);
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("download_status", "downloading");
        editor.apply();
    }

    public long getDownloadReference() {
        return downloadReference;
    }
}
