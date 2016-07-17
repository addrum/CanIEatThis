package com.adamshort.canieatthis.app.util;

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

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

public class CSVReaderAsync extends AsyncTask<File, Void, JSONObject> {
    public AsyncResponse mDelegate = null;

    private String mBarcode;

    private Activity mActivity;
    private Context mContext;
    private ProgressBar mProgressBar;

    public interface AsyncResponse {
        void processFinish(JSONObject output);
    }

    public CSVReaderAsync(String barcode, ProgressBar progressBar, Context context, Activity activity,
                          AsyncResponse delegate) {
        mBarcode = barcode;
        mProgressBar = progressBar;
        mContext = context;
        mActivity = activity;
        mDelegate = delegate;
    }

    @Override
    protected void onPreExecute() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected JSONObject doInBackground(File... files) {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            File products = files[0];
            try {
                CsvParserSettings settings = new CsvParserSettings();
                settings.getFormat().setDelimiter('\t');
                settings.setMaxCharsPerColumn(10000);
                // limits to mBarcode, name, ingredients and traces
                settings.selectFields("code", "product_name", "ingredients_text", "traces");

                CsvParser parser = new CsvParser(settings);

                // call beginParsing to read records one by one, iterator-style.
                parser.beginParsing(new FileReader(products));

                String[] info = null;
                String[] row;
                Log.d("doInBackground", "Searching for product in csv...");
                while ((row = parser.parseNext()) != null) {
                    if (StringUtils.leftPad(row[0], 13, "0").equals(mBarcode)) {
                        info = row;
                        parser.stopParsing();
                        Log.d("doInBackground", "Product found in CSV!");
                    }
                }
                if (info != null) {
                    Log.d("getBarcodeInformation", Arrays.toString(info));
                    final JSONObject product = new JSONObject();
                    try {
                        int length = info.length;
                        if (length > 0 && info[0] != null) {
                            product.put("barcode", info[0]);
                        } else {
                            product.put("barcode", "");
                        }
                        if (length > 1 && info[1] != null) {
                            product.put("product_name", info[1]);
                        } else {
                            product.put("product_name", "");
                        }
                        if (length > 2 && info[2] != null) {
                            product.put("ingredients_text", info[2]);
                        } else {
                            product.put("ingredients_text", "");
                        }
                        if (length > 3 && info[3] != null) {
                            product.put("traces", info[3]);
                        } else {
                            product.put("traces", "");
                        }
                    } catch (JSONException e) {
                        Log.e("getBarcodeInformation", "Couldn't create jsonobject: " + e.toString());
                    }
                    return product;
                }
            } catch (FileNotFoundException e) {
                Log.e("getBarcodeInformation", "Couldn't find file: " + e.toString());
                return null;
            }
            return new JSONObject();
        } else {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    5);
            return null;
        }
    }

    @Override
    protected void onPostExecute(JSONObject response) {
        if (response == null) {
            Log.d("onPostExecute", "Response was null");
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.INVISIBLE);
            }
            Log.d("onPostExecute", "Issue with response");
        } else {
            Log.i("onPostExecute", response.toString());
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
        mDelegate.processFinish(response);
    }
}
