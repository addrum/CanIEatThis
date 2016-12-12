package com.adamshort.canieatthis.app.data;

import android.content.Context;
import android.util.Log;

import com.adamshort.canieatthis.R;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class DataPasser {
    private static List<MarkerOptions> mMarkersList;
    private static List<String> mFirebaseIngredientsList;
    private static List<String> mFirebaseTracesList;

    private static DataPasser mInstance = null;
    private static LatLng mLatLng;

    private DataPasser(Context context) {
        mFirebaseIngredientsList = new ArrayList<>();
        Firebase ingredientsRef = new Firebase(context.getString(R.string.firebase_url) + "/ingredients");
        ingredientsRef.keepSynced(true);
        ingredientsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ingredientSnapshot : snapshot.getChildren()) {
                    mFirebaseIngredientsList.add(ingredientSnapshot.getKey().toLowerCase());
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
            }
        });

        mFirebaseTracesList = new ArrayList<>();
        Firebase tracesRef = new Firebase(context.getString(R.string.firebase_url) + "/traces");
        tracesRef.keepSynced(true);
        tracesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot tracesSnapshot : snapshot.getChildren()) {
                    // traces are in array so it's safe to cast to List
                    String traces = (String) tracesSnapshot.getValue();
                    mFirebaseTracesList.add(traces);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }

    public static DataPasser getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DataPasser(context);
        }
        return mInstance;
    }

    public static List<String> getFirebaseTracesList() {
        return mFirebaseTracesList;
    }

    public static List<String> getFirebaseIngredientsList() {
        return mFirebaseIngredientsList;
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

    static void addToMarkersList(MarkerOptions marker) {
        if (mMarkersList == null) {
            mMarkersList = new ArrayList<>();
        }
        if (!mMarkersList.contains(marker)) {
            mMarkersList.add(marker);
        }
    }

    public static void setLatLng(LatLng latLng) {
        mLatLng = latLng;
        Log.d("setLatLng", "LatLng is: " + latLng);
    }

    public static LatLng getLatLng() {
        return mLatLng;
    }
}
