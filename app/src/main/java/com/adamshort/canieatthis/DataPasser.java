package com.adamshort.canieatthis;

public class DataPasser {
    private boolean dairy, vegetarian, vegan, gluten;
    private boolean switchesVisible, introVisible, responseVisible, itemVisible;
    private String query;

    private static DataPasser mInstance = null;

    private DataPasser() {
        dairy = false;
        vegetarian = false;
        vegan = false;
        gluten = false;
        switchesVisible = false;
        introVisible = true;
        responseVisible = false;
        itemVisible = false;
    }

    public static DataPasser getInstance() {
        if (mInstance == null) {
            mInstance = new DataPasser();
        }
        return mInstance;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isDairy() {
        return dairy;
    }

    public void setDairy(boolean dairy) {
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
}
