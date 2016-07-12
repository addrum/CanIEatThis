package com.adamshort.canieatthis.data;

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
    private static DataPasser mInstance = null;

    private List<String> firebaseIngredientsList;

    private DataPasser(Context context) {
        firebaseIngredientsList = new ArrayList<>();
        Firebase ref = new Firebase(context.getString(R.string.firebase_url) + "/ingredients");
        ref.keepSynced(true);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ingredientSnapshot : snapshot.getChildren()) {
                    firebaseIngredientsList.add(ingredientSnapshot.getKey().toLowerCase());
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
            }
        });
    }

    public static DataPasser getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DataPasser(context);
        }
        return mInstance;
    }

    public List<String> getFirebaseIngredientsList() {
        Log.d("fbIngredientsList", firebaseIngredientsList.toString());
        return firebaseIngredientsList;
    }
}
