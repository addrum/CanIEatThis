package com.adamshort.canieatthis.app.data;

import android.util.Log;

import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class DataPasser {

    private static List<MarkerOptions> mMarkersList;

    private static DataPasser mInstance = null;

    private DataPasser() {
    }

    public static DataPasser getInstance() {
        if (mInstance == null) {
            mInstance = new DataPasser();
        }
        return mInstance;
    }

    public static List<MarkerOptions> getMarkersList() {
        if (mMarkersList == null) {
            Log.d("getMarkersList", "getMarkersList: null");
            return null;
        }
        Log.d("getMarkersList", mMarkersList.toString());
        return mMarkersList;
    }

    public static void setMarkersList(List<MarkerOptions> markersList) {
        mMarkersList = markersList;
        Log.d("setMarkersList", "markersList size: " + mMarkersList.size());
    }

    public static void addToMarkersList(MarkerOptions marker) {
        if (mMarkersList == null) {
            mMarkersList = new ArrayList<>();
        }
        if (!mMarkersList.contains(marker)) {
            mMarkersList.add(marker);
        }
    }
}
