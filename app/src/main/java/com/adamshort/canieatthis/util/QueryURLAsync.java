package com.adamshort.canieatthis.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class QueryURLAsync extends AsyncTask<String, Void, String> {

    private Context context;
    private ProgressBar progressBar;

    // based on this solution: http://stackoverflow.com/a/12575319/1860436
    public interface AsyncResponse {
        void processFinish(String output);
    }

    public AsyncResponse delegate = null;

    public QueryURLAsync(Context context, ProgressBar progressBar, AsyncResponse delegate) {
        this.context = context;
        this.progressBar = progressBar;
        this.delegate = delegate;
    }

    @Override
    protected void onPreExecute() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            URL url = new URL(urls[0]);
            Log.d("doInBackground", "Executing response at " + url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                return stringBuilder.toString();
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String response) {
        if (response == null) {
            Log.d("onPostExecute", "Response was null");
            if (progressBar != null) {
                progressBar.setVisibility(View.INVISIBLE);
            }
            Toast.makeText(context, "There was an issue finding information. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        if (response.equals("")) {
            Log.d("onPostExecute", "Couldn't find matching barcode in local csv");
            if (progressBar != null) {
                progressBar.setVisibility(View.INVISIBLE);
            }
            Toast.makeText(context, "There was an issue finding information. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.INVISIBLE);
        }
        Log.i("onPostExecute", response);
        delegate.processFinish(response);
    }

}