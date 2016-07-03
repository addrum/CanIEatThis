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

    private boolean dairy, vegetarian, vegan, gluten;
    private boolean switchesVisible, introVisible, responseVisible, itemVisible;
    private boolean fromSearch;

    private List<String> firebaseIngredientsList;
    private String query;
    private String ingredients;
    private String traces;


    private DataPasser(Context context) {
        dairy = false;
        vegetarian = false;
        vegan = false;
        gluten = false;
        switchesVisible = false;
        introVisible = true;
        responseVisible = false;
        itemVisible = false;

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

    public String getQuery() {
        Log.d("getQuery", "Query: " + query);
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isDairy() {
        return dairy;
    }

    public void setLactose(boolean dairy) {
        this.dairy = dairy;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }

    public void setVegetarian(boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public boolean isVegan() {
        return vegan;
    }

    public void setVegan(boolean vegan) {
        this.vegan = vegan;
    }

    public boolean isGluten() {
        return gluten;
    }

    public void setGluten(boolean gluten) {
        this.gluten = gluten;
    }

    public boolean areSwitchesVisible() {
        return switchesVisible;
    }

    public void setSwitchesVisible(boolean switchesVisible) {
        this.switchesVisible = switchesVisible;
    }

    public boolean isItemVisible() {
        return itemVisible;
    }

    public void setItemVisible(boolean itemVisible) {
        this.itemVisible = itemVisible;
    }

    public boolean isIntroVisible() {
        return introVisible;
    }

    public void setIntroVisible(boolean introVisible) {
        this.introVisible = introVisible;
    }

    public boolean isResponseVisible() {
        return responseVisible;
    }

    public void setResponseVisible(boolean responseVisible) {
        this.responseVisible = responseVisible;
    }

    public void setIngredients(String ingredients) {
        Log.d("setIngredients", "Ingredients: " + ingredients);
        this.ingredients = ingredients;
    }

    public String getIngredients() {
        Log.d("getIngredients", "Ingredients: " + ingredients);
        return ingredients;
    }

    public void setFromSearch(boolean fromSearch) {
        Log.d("setFromSearch", "From Search: " + fromSearch);
        this.fromSearch = fromSearch;
    }

    public boolean isFromSearch() {
        Log.d("isFromSearch", "From Search: " + fromSearch);
        return fromSearch;
    }

    public void setTraces(String traces) {
        Log.d("setTraces", "Traces: " + traces);
        this.traces = traces;
    }

    public String getTraces() {
        Log.d("getTraces", "Traces: " + traces);
        return traces;
    }

    public List<String> getFirebaseIngredientsList() {
        Log.d("fbIngredientsList", firebaseIngredientsList.toString());
        return firebaseIngredientsList;
    }
}
