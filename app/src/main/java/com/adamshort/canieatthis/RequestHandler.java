package com.adamshort.canieatthis;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class RequestHandler extends AsyncTask<String, Void, String> {

    private boolean connection = false;
    private String barcode = "";

    private Context context;
    private ProgressBar progressBar;
    private BufferedReader br = null;

    // based on this solution: http://stackoverflow.com/a/12575319/1860436
    public interface AsyncResponse {
        void processFinish(String output);
    }

    public AsyncResponse delegate = null;

    public RequestHandler(Context context, ProgressBar progressBar, AsyncResponse delegate) {
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
        if (connection) {
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
        } else {
            try {
                return parseCSV(new InputStreamReader(context.getAssets().open("products.csv")));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @Override
    protected void onPostExecute(String response) {
        if (response == null) {
            Log.d("DEBUG", "THERE WAS AN ERROR");
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(context, "There was an issue finding information. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (response.equals("")) {
            Log.d("DEBUG", "Couldn't find matching barcode in local csv");
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(context, "There was an issue finding information. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i("INFO", response);
        delegate.processFinish(response);
    }

    public String parseCSV(InputStreamReader csvFile) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        try {
            br = new BufferedReader(csvFile);
            String line;
            while ((line = br.readLine()) != null) {

                String[] row = line.split(",");

                Log.d("DEBUG:", row[0]);

//                    if (Long.toString(barcodeVal).equals(barcode)) {
//                        return Arrays.toString(row);
//                    }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    protected void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
