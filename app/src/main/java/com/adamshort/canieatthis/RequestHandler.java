package com.adamshort.canieatthis;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RequestHandler extends AsyncTask<String, Void, String> {

    String response;

    protected void onPreExecute() {
    }

    protected String doInBackground(String... urls) {
        try {
            URL url = new URL(urls[0]);
            Log.d("Response", "Executing response at " + url);
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

    protected void onPostExecute(String response) {
        if (response == null) {
            Log.d("ERROR", "THERE WAS AN ERROR");
            return;
        }
        Log.i("INFO", response);

        this.response = response;
    }

    protected String getResponse() {
        return response;
    }
}
