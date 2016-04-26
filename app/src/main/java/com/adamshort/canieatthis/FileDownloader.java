package com.adamshort.canieatthis;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.os.Environment.getExternalStorageDirectory;

public class FileDownloader extends AsyncTask<String, Void, String> {

    private Activity activity;
    private Context context;
    private ProgressBar progressBar;

    // based on this solution: http://stackoverflow.com/a/12575319/1860436
    public interface AsyncResponse {
        void processFinish(String output);
    }

    public AsyncResponse delegate = null;

    public FileDownloader(Activity activity, Context context, ProgressBar progressBar, AsyncResponse delegate) {
        this.activity = activity;
        this.context = context;
        this.progressBar = progressBar;
        this.delegate = delegate;
    }

    protected void onPreExecute() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    protected String doInBackground(String... urls) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urls[0]);
            Log.d("Response", "Executing response at " + url);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
            }

            // download the file
            input = connection.getInputStream();
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                output = new FileOutputStream(String.format("%s/products.csv", getExternalStorageDirectory().getPath()));
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return null;
            }

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String response) {
        if (response == null || response.equals("")) {
            Log.d("DEBUG", "Response was null");
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(context, "There was an issue downloading database. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        progressBar.setVisibility(View.INVISIBLE);
        Log.i("INFO", response);
        delegate.processFinish(response);
    }
}
