package com.adamshort.canieatthis.wear.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.adamshort.canieatthis.wear.util.QueryURLAsync;
import com.example.canieatthiswear.R;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MainWearActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;

    private static final String placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=";

    private static String radius = "1000";
    private static String nextPageToken;

    private boolean connected;
    private boolean mapReady;
    private double lat;
    private double lng;
    private String apiKey;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private MapView mMapView;
    private DismissOverlayView mDismissOverlay;

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Set the layout. It only contains a SupportMapFragment and a DismissOverlay.
        setContentView(R.layout.activity_main_wear);

        if (!Firebase.getDefaultConfig().isPersistenceEnabled()) {
            Firebase.getDefaultConfig().setPersistenceEnabled(true);
        }
        Firebase.setAndroidContext(getBaseContext());

        apiKey = getResources().getString(R.string.server_key);

        // Enable ambient support, so the map remains visible in simplified, low-color display
        // when the user is no longer actively using the app but the app is still visible on the
        // watch face.
        setAmbientEnabled();

        // Retrieve the containers for the root of the layout and the map. Margins will need to be
        // set on them to account for the system window insets.
        final FrameLayout topFrameLayout = (FrameLayout) findViewById(R.id.root_container);
        final FrameLayout mapFrameLayout = (FrameLayout) findViewById(R.id.map_container);

        // Set the system view insets on the containers when they become available.
        topFrameLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Call through to super implementation and apply insets
                insets = topFrameLayout.onApplyWindowInsets(insets);

                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mapFrameLayout.getLayoutParams();

                // Add Wearable insets to FrameLayout container holding map as margins
                params.setMargins(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());
                mapFrameLayout.setLayoutParams(params);

                return insets;
            }
        });

        // Obtain the DismissOverlayView and display the intro help text.
        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText(R.string.wearExitText);
        mDismissOverlay.showIntroIfNecessary();

        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.onCreate(savedState);
        mMapView.onResume(); // needed to get the map to display immediately
        mMapView.getMapAsync(this);

        createGoogleAPIClient();
    }

    private void moveCamera(GoogleMap googleMap, LatLng latLng) {
        if (latLng != null && googleMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng).zoom(15).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private LatLng getUserLatLng() {
        if (checkForPermission()) {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            try {
                lat = mLastLocation.getLatitude();
                lng = mLastLocation.getLongitude();
            } catch (NullPointerException e) {
                Log.e("getUserLatLng", "Couldn't get lat or long from last location: " + e.toString());
            }
            return new LatLng(lat, lng);
        }
        return null;
    }

    private void createGoogleAPIClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                    .addApi(LocationServices.API)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    private void createNearbyMarkers(GoogleMap googleMap) {
        if (googleMap != null) {
            googleMap.clear();
        }
        if (checkForPermission()) {
            if (lat != 0 && lng != 0) {
                String url = placesUrl + lat + "," + lng + "&radius=" + radius + "&type=restaurant&key=" + apiKey;
                queryPlacesURL(url);
            }
        } else {
            Log.d("createNearbyMarkers", "LatLng was null so won't make places request");
        }
    }

    private void queryPlacesURL(String placesUrl) {
        QueryURLAsync rh = new QueryURLAsync(getBaseContext(), null, new QueryURLAsync.AsyncResponse() {
            @Override
            public void processFinish(String output) {
                try {
                    JSONObject response = new JSONObject(output);
                    JSONArray results = response.getJSONArray("results");
                    try {
                        MainWearActivity.nextPageToken = response.getString("next_page_token");
                        if (!nextPageToken.equals("")) {
//                            showMoreButton.setVisibility(View.VISIBLE);
                        }
                    } catch (JSONException e) {
                        nextPageToken = "";
//                        showMoreButton.setVisibility(View.INVISIBLE);
                    }

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject location = results.getJSONObject(i);
                        createMarker(location);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        rh.execute(placesUrl);
    }

    private void createMarker(JSONObject location) {
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
                boolean openNow = location.getJSONObject("opening_hours").getBoolean("open_now");
                if (openNow) {
                    snippetText += "Open Now: Yes";
                } else {
                    snippetText += "Open Now: No";
                }
            } catch (JSONException e) {
                Log.e("processFinish", "No value for opening_hours or open_now");
            }

            try {
                String rating = location.getString("rating");
                snippetText += ",Rating: " + rating;
            } catch (JSONException e) {
                Log.e("processFinish", "No value for rating");
            }

            marker.snippet(snippetText);
            marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));

            FirebaseAsyncRequest fb = new FirebaseAsyncRequest();
            fb.execute(marker);

            Log.d("DEBUG", "Name: " + name + " lat " + lat + " lng " + lng);
        } catch (JSONException e) {
            Log.e("createMarker", e.toString());
        }
    }

    /**
     * Starts ambient mode on the map.
     * The API swaps to a non-interactive and low-color rendering of the map when the user is no
     * longer actively using the app.
     */
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mMapView.onEnterAmbient(ambientDetails);
    }

    /**
     * Exits ambient mode on the map.
     * The API swaps to the normal rendering of the map when the user starts actively using the app.
     */
    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mMapView.onExitAmbient();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("onMapReady", "Map is ready");
        mapReady = true;
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        if (connected) {
            getUserLatLng();
        }
        moveCamera(googleMap, new LatLng(lat, lng));

        if (checkForPermission()) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            createNearbyMarkers(googleMap);
        }

        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                getUserLatLng();
//                createNearbyMarkers(googleMap);
                return false;
            }
        });
    }

    private class FirebaseAsyncRequest extends AsyncTask<MarkerOptions, Void, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(MarkerOptions... params) {
            final MarkerOptions marker = params[0];
            Firebase ref = new Firebase(getString(R.string.firebase_url) + "/places");
            ref.keepSynced(true);
            ref.addValueEventListener(new ValueEventListener() {
                @SuppressWarnings("unchecked")
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    LatLng markerLatLng = marker.getPosition();
                    String snippet = marker.getSnippet();
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
                                        snippet += ",Lactose Free: ";
                                        if (lactose_true > lactose_false) {
                                            snippet += "Yes";
                                        } else {
                                            snippet += "No";
                                        }
                                    }

                                    Map<String, Object> vegetarian = loc.get("vegetarian");
                                    long vegetarian_true = getKeyValue(vegetarian, "true");
                                    long vegetarian_false = getKeyValue(vegetarian, "false");
                                    if (shouldShowInfo(vegetarian_true, vegetarian_false)) {
                                        snippet += ",Vegetarian: ";
                                        if (vegetarian_true > vegetarian_false) {
                                            snippet += "Yes";
                                        } else {
                                            snippet += "No";
                                        }
                                    }

                                    Map<String, Object> vegan = loc.get("vegan");
                                    long vegan_true = getKeyValue(vegan, "true");
                                    long vegan_false = getKeyValue(vegan, "false");
                                    if (shouldShowInfo(vegan_true, vegan_false)) {
                                        snippet += ",Vegan: ";
                                        if (vegan_true > vegan_false) {
                                            snippet += "Yes";
                                        } else {
                                            snippet += "No";
                                        }
                                    }

                                    Map<String, Object> gluten = loc.get("gluten_free");
                                    long gluten_true = getKeyValue(gluten, "true");
                                    long gluten_false = getKeyValue(gluten, "false");
                                    if (shouldShowInfo(gluten_true, gluten_false)) {
                                        snippet += ",Gluten Free: ";
                                        if (gluten_true > gluten_false) {
                                            snippet += "Yes";
                                        } else {
                                            snippet += "No";
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("onDataChange", "Couldn't get key from Map: " + e.toString());
                                }
                            }
                        }
                    }
                    marker.snippet(snippet);
                    mMap.addMarker(marker);
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

    private long getKeyValue(Map<String, Object> dietary, String key) {
        long value = 0;
        try {
            value = (long) dietary.get(key);
        } catch (Exception e) {
            Log.e("getKeyValue", "Issue getting " + key + "from dietary requirement");
        }
        return value;
    }

    private boolean shouldShowInfo(double true_value, double false_value) {
        double ratio = 1;
        if (true_value > 0 && false_value > 0) {
            if (true_value < false_value) {
                ratio = true_value / false_value;
            } else {
                ratio = false_value / true_value;
            }
        } else if (!(true_value == 0 && false_value == 0)) {
            if (true_value == 0) {
                if (ratio < false_value) {
                    ratio /= false_value;
                } else {
                    false_value /= ratio;
                }
            } else {
                if (ratio < true_value) {
                    ratio /= true_value;
                } else {
                    true_value /= ratio;
                }
            }
        }
        Log.d("shouldShowInfo", "ratio: " + ratio);
        return ratio < 0.2 && (true_value > 5 || false_value > 5);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        // Display the dismiss overlay with a button to exit this activity.
        mDismissOverlay.show();
    }

    private boolean checkForPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("checkForPermission", "Didn't have needed permission, requesting ACCESS_FINE_LOCATION");
            ActivityCompat.requestPermissions(MainWearActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            return false;
        }
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "APIClient connected");
        connected = true;

        if (checkForPermission()) {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (loc != null) {
                lat = loc.getLatitude();
                lng = loc.getLongitude();
            }
            if (mapReady && mMap != null) {
                moveCamera(mMap, getUserLatLng());
                createNearbyMarkers(mMap);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d("onRequestPermissions", "Permissions have been requested");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (connected) {
                        if (checkForPermission()) {
                            moveCamera(mMap, getUserLatLng());
                            mMap.setMyLocationEnabled(true);
                            mMap.getUiSettings().setMyLocationButtonEnabled(true);
                            createNearbyMarkers(mMap);
                        }
                    }
                }
            }
        }
        // other 'case' lines to check for other
        // permissions this app might request
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
            moveCamera(mMap, getUserLatLng());
            createNearbyMarkers(mMap);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
        if (mMap != null) {
            mMap.clear();
        }
        mapReady = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        if (mMap != null) {
            mMap.clear();
        }
        connected = false;
        mapReady = false;
    }
}
