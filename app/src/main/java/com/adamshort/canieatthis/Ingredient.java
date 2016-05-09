package com.adamshort.canieatthis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Ingredient {
    private boolean dairy_free;
    private boolean vegetarian;
    private boolean vegan;
    private boolean gluten_free;

    public Ingredient() {}

    public boolean getDairyFree() {
        return dairy_free;
    }

    public boolean getVegetarian() {
        return vegetarian;
    }

    public boolean getVegan() {
        return vegan;
    }

    public boolean getGlutenFree() {
        return gluten_free;
    }
}
