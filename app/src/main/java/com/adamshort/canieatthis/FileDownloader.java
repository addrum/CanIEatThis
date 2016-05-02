package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class FileDownloader {

    private long downloadReference;

    public FileDownloader(Activity activity, DownloadManager downloadManager, String url, String filename) {
        Log.d("DEBUG", "Download file at: " + url);
        Uri uri = Uri.parse(url);

        String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(storageState)) {

            File file = new File(activity.getExternalFilesDir(null).getPath(), filename);
            if (file.exists()) {
                Log.d("DEBUG", "File exists!");
                boolean delete = file.delete();
                Log.d("DEBUG", "Deleted: " + delete);
            }

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setDescription("Local database for CanIEatThis");
            request.setTitle("CanIEatThis Database");
            request.setDestinationInExternalFilesDir(activity, null, filename);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);

            downloadReference = downloadManager.enqueue(request);
            SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("download_status", "downloading");
            editor.apply();
        } else {
            Toast.makeText(activity, "Storage device not available", Toast.LENGTH_LONG).show();
            Log.e("ERROR", "Storage device was not available, state was: " + storageState);
        }
    }

    public long getDownloadReference() {
        return downloadReference;
    }
}
