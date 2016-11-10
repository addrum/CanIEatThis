package com.adamshort.canieatthis.app.util;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class QueryURLAsync extends AsyncTask<String, Void, String> {
    public AsyncResponse mDelegate = null;

    private int mSleepAmount;

    private ProgressBar mProgressBar;

    // based on this solution: http://stackoverflow.com/a/12575319/1860436
    public interface AsyncResponse {
        void processFinish(String output);
    }

    public QueryURLAsync(ProgressBar progressBar, int sleepAmount, AsyncResponse delegate) {
        mProgressBar = progressBar;
        mSleepAmount = sleepAmount;
        mDelegate = delegate;
    }

    @Override
    protected void onPreExecute() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        Log.d("onPreExecute", "Sleep amount: " + mSleepAmount);
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            Thread.sleep(mSleepAmount);
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
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.INVISIBLE);
            }
            Log.d("onPostExecute", "Issue with response");
        } else if (response.equals("")) {
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.INVISIBLE);
            }
            Log.d("onPostExecute", "Issue with response");
        } else {
            Log.i("onPostExecute", response);
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
        mDelegate.processFinish(response);
    }

}
