package com.adamshort.canieatthis.app.data;

import android.content.Context;
import android.util.Log;

import com.adamshort.canieatthis.R;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DataPasser {
    private static List<String> mFirebaseIngredientsList;
    private static List<String> mFirebaseTracesList;

    private static DataPasser mInstance = null;

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
        Log.d("fbTracesList", mFirebaseTracesList.toString());
        return mFirebaseTracesList;
    }

    public List<String> getFirebaseIngredientsList() {
        Log.d("fbIngredientsList", mFirebaseIngredientsList.toString());
        return mFirebaseIngredientsList;
    }
}
