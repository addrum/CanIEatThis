package com.adamshort.canieatthis;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CSVReader extends AsyncTask<String, Void, JSONObject> {

    private Context context;
    private ProgressBar progressBar;

    // based on this solution: http://stackoverflow.com/a/12575319/1860436
    public interface AsyncResponse {
        void processFinish(JSONObject output);
    }

    public AsyncResponse delegate = null;

    public CSVReader(Context context, ProgressBar progressBar, AsyncResponse delegate) {
        this.context = context;
        this.progressBar = progressBar;
        this.delegate = delegate;
    }

    protected void onPreExecute() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    protected JSONObject doInBackground(String... barcode) {
        try {
            return parseCSV(new InputStreamReader(context.getAssets().open("products.csv")), barcode[0], progressBar);
        } catch (IOException e) {
            Log.e("ERROR", "Couldn't parse CSV");
        }
        return null;
    }

    @Override
    protected void onPostExecute(JSONObject response) {
        if (response == null) {
            Log.d("DEBUG", "Response was null");
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(context, "There was an issue finding information. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        progressBar.setVisibility(View.INVISIBLE);
        Log.i("INFO", response.toString());
        delegate.processFinish(response);
    }

    public JSONObject ArrayToJSON(String[] response) {
        try {
            JSONObject convertedResponse = new JSONObject();
            if (response.length > 7) {
                convertedResponse.put("product_name", response[7]);
            }
            if (response.length > 34) {
                convertedResponse.put("ingredients_text", response[34]);
            }
            if (response.length > 37) {
                convertedResponse.put("traces", response[37]);
            }
            return convertedResponse;
        } catch (JSONException e1) {
            Log.e("ERROR", "Error setting json object with product data from csv: " + e1);
        }
        return null;
    }

    public JSONObject parseCSV(InputStreamReader csvFile, String barcode, ProgressBar progressBar) {
        BufferedReader br = null;
        String[] response = null;
        try {
            br = new BufferedReader(csvFile);
            String line;
            while ((line = br.readLine()) != null) {

                String[] row = line.split("\t");

                if (row[0].equals(barcode)) {
                    response = row;
                }
            }
            return ArrayToJSON(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
