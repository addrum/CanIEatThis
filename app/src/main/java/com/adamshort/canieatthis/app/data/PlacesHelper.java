package com.adamshort.canieatthis.app.data;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.util.NextPageListener;
import com.adamshort.canieatthis.app.util.PreferencesHelper;
import com.adamshort.canieatthis.app.util.QueryURLAsync;
import com.adamshort.canieatthis.app.util.SendToDataLayerThread;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlacesHelper implements GoogleApiClient.ConnectionCallbacks {

    private static String mNextPageToken;

    private boolean mSendToWear;
    private List<NextPageListener> mNextPageListeners;

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;

    public PlacesHelper(Context context, GoogleMap map, boolean sendToWear) {
        mContext = context;
        mMap = map;
        mSendToWear = sendToWear;

        createGoogleAPIClient(context);

        mNextPageListeners = new ArrayList<>();
    }

    /**
     * Creates a Google API Client which is needed for using the Places API.
     */
    private void createGoogleAPIClient(Context context) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addApiIfAvailable(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    public void doPlacesAPIRequest(String url, double lat, double lng) {
        if (lat != 0 && lng != 0) {
            queryPlacesURL(url);
        } else {
            Log.d("createNearbyMarkers", "mLat mLng were 0");
        }
    }

    /**
     * Asynchronously sends a Places API request, setting the mNextPageToken and creating a marker at
     * each location returned in the JSONObject.
     *
     * @param placesUrl The URL to send an HTTP request to.
     */
    private void queryPlacesURL(String placesUrl) {
        QueryURLAsync rh = new QueryURLAsync(null, 0, new QueryURLAsync.AsyncResponse() {
            @Override
            public void processFinish(String output) {
                if (output != null) {
                    try {
                        JSONObject response = new JSONObject(output);
                        JSONArray results = response.getJSONArray("results");
                        try {
                            mNextPageToken = response.getString("next_page_token");
                            showNextPage(View.VISIBLE);
                        } catch (JSONException e) {
                            mNextPageToken = "";
                            showNextPage(View.INVISIBLE);
                        }

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject location = results.getJSONObject(i);
                            createMarker(location, null);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i("processFinish", "Output was null! Might be offline");
                }
            }
        });
        rh.execute(placesUrl);
    }

    /**
     * Creates a singular marker at a specified location taken from the place's JSONObject.
     *
     * @param location JSONObject of information of a location.
     * @param m        The marker options to modify and create a marker on the map
     */
    public void createMarker(JSONObject location, MarkerOptions m) {
        if (location != null) {
            try {
                String name = location.getString("name");

                JSONObject geometry = location.getJSONObject("geometry").getJSONObject("location");
                double lat = geometry.getDouble("lat");
                double lng = geometry.getDouble("lng");

                LatLng latlng = new LatLng(lat, lng);
                MarkerOptions marker = new MarkerOptions().position(latlng)
                        .title(name);

                String snippetText = "";
                try {
                    String rating = location.getString("rating");
                    snippetText += "Rating: " + rating;
                } catch (JSONException e) {
                    Log.w("processFinish", "No value for rating");
                }

                try {
                    boolean openNow = location.getJSONObject("opening_hours").getBoolean("open_now");
                    if (openNow) {
                        snippetText += ",Open Now: Yes";
                    } else {
                        snippetText += ",Open Now: No";
                    }
                } catch (JSONException e) {
                    Log.w("processFinish", "No value for opening_hours or open_now");
                }

                marker.snippet(snippetText);
                marker.icon(BitmapDescriptorFactory.defaultMarker());

                FirebaseAsyncRequest fb = new FirebaseAsyncRequest(false);
                fb.execute(marker);

                Log.d("createMarker", "Name: " + name + " lat " + lat + " lng " + lng);
            } catch (JSONException e) {
                Log.e("createMarker", e.toString());
            }
        } else {
            FirebaseAsyncRequest fb = new FirebaseAsyncRequest(true);
            fb.execute(m);
        }
    }

    /**
     * Calculates a ratio that is used to determine if the info about a place should be displayed.
     * If the number of values already submitted for both true and false is greater than a threshold (5)
     * and if the ratio of the true to false values is less than 20% then we can assume that the information
     * provided by users is corrected, and therefore should show info.
     *
     * @param true_value  The true values submitted by users stored in firebase.
     * @param false_value The false values submitted by users stored in firbase.
     * @return True or False based on the ratio and the threshold of the true and false values.
     */
    private boolean shouldShowInfo(double true_value, double false_value) {
        double ratio = 1;
        if (true_value > 0 && false_value > 0) {
            if (true_value < false_value) {
                ratio = true_value / false_value;
            } else {
                ratio = false_value / true_value;
            }
        }
        Log.d("shouldShowInfo", "ratio: " + ratio);
        return ratio > 0.6;
    }

    /**
     * Gets the value of a specified key.
     *
     * @param dietary The object of a place which is stored in firebase.
     * @param key     The key to return the value of.
     * @return The value of the specified key.
     */
    private long getKeyValue(Map<String, Object> dietary, String key) {
        long value = 0;
        try {
            value = (long) dietary.get(key);
        } catch (Exception e) {
            Log.d("getKeyValue", "Issue getting " + key + " from dietary requirement");
        }
        return value;
    }

    public void showNextPage(int visibility) {
        Log.d("showNextPage", "Notifying listeners to show 'show more' button");

        for (NextPageListener listener : mNextPageListeners) {
            listener.showMore(visibility);
        }
    }

    public void addNextPageListener(NextPageListener listener) {
        mNextPageListeners.add(listener);
    }

    public String getNextPageToken() {
        return mNextPageToken;
    }

    /**
     * Queries firebase asynchronously and adds information about the place to the custom info marker.
     */
    private class FirebaseAsyncRequest extends AsyncTask<MarkerOptions, Void, String> {

        private boolean mIsFromCache;

        public FirebaseAsyncRequest(boolean isFromCache) {
            mIsFromCache = isFromCache;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(MarkerOptions... params) {
            final MarkerOptions marker = params[0];
            Firebase ref = new Firebase(mContext.getString(R.string.firebase_url) + "/places");
            ref.keepSynced(true);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressWarnings("unchecked")
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!mIsFromCache) {
                        Boolean[] bools = new Boolean[]{null, null, null, null};
                        LatLng markerLatLng = marker.getPosition();
                        String snippet = marker.getSnippet();
                        Pattern lactosePattern = Pattern.compile("Lactose Free:\\s(\\w*)");
                        Pattern vegetarianPattern = Pattern.compile("Vegetarian:\\s(\\w*)");
                        Pattern veganPattern = Pattern.compile("Vegan:\\s(\\w*)");
                        Pattern glutenPattern = Pattern.compile("Gluten Free:\\s(\\w*)");

                        for (DataSnapshot location : snapshot.getChildren()) {
                            // Firebase doesn't allow . in key's so had to submit as ,
                            // so now we need to replace it so we can get it back to latlng
                            String[] key = location.getKey().replace(",", ".").split(" ");
                            LatLng locLatLng = null;
                            try {
                                locLatLng = new LatLng(Double.parseDouble(key[0]), Double.parseDouble(key[1]));
                            } catch (NumberFormatException e) {
                                Log.e("onDataChange", e.toString());
                            }

                            if (locLatLng != null) {
                                if (markerLatLng.equals(locLatLng)) {
                                    Map<String, Map<String, Object>> loc = (Map<String, Map<String, Object>>) location.getValue();
                                    Log.d("onDataChange", loc.toString());
                                    try {
                                        Map<String, Object> lactose = loc.get("lactose_free");
                                        long lactose_true = getKeyValue(lactose, "true");
                                        long lactose_false = getKeyValue(lactose, "false");
                                        if (shouldShowInfo(lactose_true, lactose_false)) {
                                            snippet = addToSnippet(lactosePattern, snippet,
                                                    lactose_true, lactose_false, "Lactose Free");
                                        }
                                        bools[0] = lactose_true > lactose_false;

                                        Map<String, Object> vegetarian = loc.get("vegetarian");
                                        long vegetarian_true = getKeyValue(vegetarian, "true");
                                        long vegetarian_false = getKeyValue(vegetarian, "false");
                                        if (shouldShowInfo(vegetarian_true, vegetarian_false)) {
                                            snippet = addToSnippet(vegetarianPattern, snippet,
                                                    vegetarian_true, vegetarian_false, "Vegetarian");
                                        }
                                        bools[1] = vegetarian_true > vegetarian_false;

                                        Map<String, Object> vegan = loc.get("vegan");
                                        long vegan_true = getKeyValue(vegan, "true");
                                        long vegan_false = getKeyValue(vegan, "false");
                                        if (shouldShowInfo(vegan_true, vegan_false)) {
                                            snippet = addToSnippet(veganPattern, snippet,
                                                    vegan_true, vegan_false, "Vegan");
                                        }
                                        bools[2] = vegan_true > vegan_false;

                                        Map<String, Object> gluten = loc.get("gluten_free");
                                        long gluten_true = getKeyValue(gluten, "true");
                                        long gluten_false = getKeyValue(gluten, "false");
                                        if (shouldShowInfo(gluten_true, gluten_false)) {
                                            snippet = addToSnippet(glutenPattern, snippet,
                                                    gluten_true, gluten_false, "Gluten Free");
                                        }
                                        bools[3] = gluten_true > gluten_false;
                                    } catch (Exception e) {
                                        Log.e("onDataChange", "Couldn't get key from Map: " + e.toString());
                                    }
                                }
                            }
                        }
                        if (!snippet.contains("Lactose Free")
                                && !snippet.contains("Vegetarian")
                                && !snippet.contains("Vegan")
                                && !snippet.contains("Gluten Free")) {
                            if (snippet.endsWith("Yes") || snippet.endsWith("No")) {
                                snippet += ",";
                            }
                            snippet += mContext.getString(R.string.noInfoOnPlace);
                        }
                        marker.snippet(snippet);
                        addMarker(marker, bools);
                    } else {
                        addMarkerFromCache(marker);
                    }
                }

                @Override
                public void onCancelled(FirebaseError error) {
                }
            });
            return "Successful firebase request";
        }

        @Override
        protected void onPostExecute(String response) {
        }

    }

    private String addToSnippet(Pattern pattern, String snippet, long trueValue, long falseValue, String dietary) {
        Matcher m = pattern.matcher(snippet);
        StringBuffer sb = new StringBuffer();
        if (m.find()) {
            if (trueValue > falseValue) {
                m.appendReplacement(sb, snippet.replace(m.group(1), "Yes"));
            } else {
                m.appendReplacement(sb, snippet.replace(m.group(1), "No"));
            }
            snippet = sb.toString();
        } else {
            snippet += "," + dietary + ": ";
            if (trueValue > falseValue) {
                snippet += "Yes";
            } else {
                snippet += "No";
            }
        }
        Log.d("addToSnippet", "Snippet: " + snippet);
        return snippet;
    }

    private void addMarker(MarkerOptions marker, Boolean[] bools) {
        Log.d("addMarker", "Adding marker: " + marker.getTitle());

        boolean mLactosePref = PreferencesHelper.getLactoseFreePref(mContext);
        boolean mVegetarianPref = PreferencesHelper.getVegetarianPref(mContext);
        boolean mVeganPref = PreferencesHelper.getVeganPref(mContext);
        boolean mGlutenPref = PreferencesHelper.getGlutenFreePref(mContext);

        Boolean lac = bools[0];
        Boolean veg = bools[1];
        Boolean vegan = bools[2];
        Boolean glu = bools[3];

        if ((!mLactosePref && !mVegetarianPref && !mVeganPref && !mGlutenPref)
                || (mLactosePref && (lac == null || lac))
                || (mVegetarianPref && (veg == null || veg))
                || (mVeganPref && (vegan == null || vegan))
                || (mGlutenPref && (glu == null || glu))) {
            if (mSendToWear) {
                try {
                    JSONObject newMarker = new JSONObject();
                    newMarker.put("name", marker.getTitle());

                    JSONObject location = new JSONObject();
                    LatLng latLng = marker.getPosition();
                    location.put("lat", latLng.latitude);
                    location.put("lng", latLng.longitude);
                    newMarker.put("location", location);

                    newMarker.put("snippet", marker.getSnippet());

                    sendMarkersToWear(newMarker.toString());
                } catch (JSONException e) {
                    Log.e("addMarker", "Couldn't create json for marker to send to wear");
                }
            } else {
                mMap.addMarker(marker);
            }
        } else {
            Log.i("onDataChange", "Not adding marker. Name: " + marker.getTitle()
                    + " mLactosePref: " + mLactosePref
                    + " mVegetarianPref: " + mVegetarianPref
                    + " mVeganPref: " + mVeganPref
                    + " mGlutenPref: " + mGlutenPref
                    + " lac: " + lac
                    + " veg: " + veg
                    + " vegan: " + vegan
                    + " glu: " + glu);
        }
        DataPasser.getInstance(mContext);
        DataPasser.addToMarkersList(marker);
    }

    private void addMarkerFromCache(MarkerOptions marker) {
        Log.d("addMarkerFromCache", "Adding marker from cache: " + marker.getTitle());
        String snippet = marker.getSnippet();

        Pattern lactosePattern = Pattern.compile("Lactose Free: ([a-zA-z]*)");
        Matcher m = lactosePattern.matcher(snippet);
        Boolean lactose = null;
        if (m.find()) {
            if (m.group(1).equals("Yes")) {
                lactose = true;
            } else if (m.group(1).equals("No")) {
                lactose = false;
            }
        }

        Pattern vegetarianPattern = Pattern.compile("Vegetarian: ([a-zA-z]*)");
        m = vegetarianPattern.matcher(snippet);
        Boolean vegetarian = null;
        if (m.find()) {
            if (m.group(1).equals("Yes")) {
                vegetarian = true;
            } else if (m.group(1).equals("No")) {
                vegetarian = false;
            }
        }

        Pattern veganPattern = Pattern.compile("Vegan: ([a-zA-z]*)");
        m = veganPattern.matcher(snippet);
        Boolean vegan = null;
        if (m.find()) {
            if (m.group(1).equals("Yes")) {
                vegan = true;
            } else if (m.group(1).equals("No")) {
                vegan = false;
            }
        }

        Pattern glutenPattern = Pattern.compile("Gluten Free: ([a-zA-z]*)");
        m = glutenPattern.matcher(snippet);
        Boolean gluten = null;
        if (m.find()) {
            if (m.group(1).equals("Yes")) {
                gluten = true;
            } else if (m.group(1).equals("No")) {
                gluten = false;
            }
        }

        addMarker(marker, new Boolean[]{lactose, vegetarian, vegan, gluten});
    }

    private void sendMarkersToWear(String markers) {
        Log.i("sendMarkersToWear", "Sending message to wear");
        new SendToDataLayerThread("/watch_path", markers, mGoogleApiClient).start();
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

}
