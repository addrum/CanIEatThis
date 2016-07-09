package com.adamshort.canieatthis.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.data.Installation;
import com.adamshort.canieatthis.ui.PopupAdapter;
import com.adamshort.canieatthis.ui.activity.AddPlacesInfoActivity;
import com.adamshort.canieatthis.util.QueryURLAsync;
import com.adamshort.canieatthis.util.Utilities;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class PlacesFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int FORM_REQUEST_CODE = 11;
    private static boolean fromSearch;

    private static String radius = "1000";
    private static String nextPageToken;

    private boolean placesRequestSubmitted;
    private boolean isGoogleConnected;
    private boolean isVisible;
    private boolean isMapSetup;
    private double lat;
    private double lng;
    private String apiKey;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private MapView mMapView;
    private CoordinatorLayout coordinatorLayout;
    private Button showMoreButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate and return the layout
        View v = inflater.inflate(R.layout.fragment_places, container, false);
        setHasOptionsMenu(true);
        apiKey = getResources().getString(R.string.server_key);

        coordinatorLayout = (CoordinatorLayout) v.findViewById(R.id.places_coordinator_layout);

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.onCreate(mapViewSavedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately
        mMapView.getMapAsync(this);

        showMoreButton = (Button) v.findViewById(R.id.showMoreButton);
        showMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMore(nextPageToken);
                showMoreButton.setVisibility(View.INVISIBLE);
            }
        });

        Button searchButton = (Button) v.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                    .build(getActivity());
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.e("onClick", "error starting place autocomplete: " + e.toString());
                }
            }
        });

        //HACK: Get the button view and place it on the bottom right (as Google Maps app)
        //noinspection ResourceType
        View locationButton = ((View) v.findViewById(1).getParent()).findViewById(2);
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 200); // left, top, right, bottom

        try {
            MapsInitializer.initialize(getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Perform any camera updates here
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //This MUST be done before saving any of your own or your base class's variables
        final Bundle mapViewSaveState = new Bundle(outState);
        mMapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle("mapViewSaveState", mapViewSaveState);
        //Add any other variables here.
        super.onSaveInstanceState(outState);
    }

    private void createGoogleAPIClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
            isGoogleConnected = true;
        }
    }

    private void moveCamera(GoogleMap googleMap, LatLng latLng) {
        if ((isVisible || !Utilities.isPortraitMode(getContext())) && googleMap != null && latLng != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng).zoom(15).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            Log.d("moveCamera", "Moving camera to: " + cameraPosition);
        }
    }

    private void setUpMap() {
        createGoogleAPIClient();
        if (mMap != null) {
            if (isVisible || !Utilities.isPortraitMode(getContext())) {
                checkLocationPermission();
                LatLng latLng = getLatLng();
                if (latLng.latitude != 0 && latLng.longitude != 0) {
                    moveCamera(mMap, getLatLng());

                    createNearbyMarkers(mMap);
                    if (fromSearch) {
                        createCustomMarker(getLatLng());
                    }
                }
                isMapSetup = true;
            }
        }
        placesRequestSubmitted = false;
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d("onMapReady", "Map is ready");
        mMap = googleMap;

        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                setUserLocation();
                createNearbyMarkers(googleMap);
                fromSearch = false;
                return false;
            }
        });

        googleMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater(getArguments())));

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                try {
                    Installation.id(getContext());
                    if (!Installation.isInInstallationFile(getContext(),
                            marker.getPosition().toString())) {
                        showMoreButton.setVisibility(View.INVISIBLE);
                        Intent intent = new Intent(getContext(), AddPlacesInfoActivity.class);
                        intent.putExtra("name", marker.getTitle());
                        intent.putExtra("latlng", marker.getPosition().toString());
                        startActivityForResult(intent, FORM_REQUEST_CODE);
                    } else {
                        Snackbar.make(coordinatorLayout, "You have already submitted information about this place"
                                , Snackbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e("onInfoWindowClick", "issue checking if latlng is in install file: " + e.toString());
                }
            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return marker.getTitle().equals("custom");
            }
        });
    }

    private void createNearbyMarkers(GoogleMap googleMap) {
        if (isGoogleConnected) {
            if (googleMap != null) {
                googleMap.clear();
            }
            if (lat != 0 && lng != 0) {
                String url = getString(R.string.placesUrl) + lat + "," + lng + "&radius=" + radius + "&type=restaurant&key=" + apiKey;
                queryPlacesURL(url);
            } else {
                Log.d("createNearbyMarkers", "lat lng were 0");
            }
        } else {
            Log.d("createNearbyMarkers", "Not connected so can't make places request");
        }
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
            marker.icon(BitmapDescriptorFactory.defaultMarker());

            FirebaseAsyncRequest fb = new FirebaseAsyncRequest();
            fb.execute(marker);

            Log.d("DEBUG", "Name: " + name + " lat " + lat + " lng " + lng);
        } catch (JSONException e) {
            Log.e("createMarker", e.toString());
        }
    }

    private void showMore(String nextPageToken) {
        if (!nextPageToken.equals("")) {
            String url = getString(R.string.placesUrl) + lat + "," + lng + "&radius=" + radius + "&type=restaurant&key=" + apiKey
                    + "&pagetoken=" + nextPageToken;
            queryPlacesURL(url);
        }
        Log.d("showMore", "Next page token was null, won't show more");
    }

    private void queryPlacesURL(String placesUrl) {
        QueryURLAsync rh = new QueryURLAsync(getContext(), null, new QueryURLAsync.AsyncResponse() {
            @Override
            public void processFinish(String output) {
                try {
                    JSONObject response = new JSONObject(output);
                    JSONArray results = response.getJSONArray("results");
                    try {
                        PlacesFragment.nextPageToken = response.getString("next_page_token");
                        if (!nextPageToken.equals("")) {
                            showMoreButton.setVisibility(View.VISIBLE);
                        }
                    } catch (JSONException e) {
                        nextPageToken = "";
                        showMoreButton.setVisibility(View.INVISIBLE);
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

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("checkLocationPref", "permission already granted");
            setUserLocationSettings();
        } else {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("checkLocationPer", "should show request permission rationale");
                // Need to show permission rationale, display a snackbar and then request
                // the permission again when the snackbar is dismissed.
                Snackbar.make(coordinatorLayout,
                        R.string.fineLocationRationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.d("checkLocationPer", "request permission");
                                // Request the permission again.
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSION_ACCESS_FINE_LOCATION);
                            }
                        }).show();
            } else {
                int timesAsked = Utilities.getTimesAskedForPermPref(getContext());
                if (timesAsked < 2) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSION_ACCESS_FINE_LOCATION);
                    timesAsked += 1;
                    Utilities.setTimesAskedForPermPref(getContext(), timesAsked);
                } else {
                    Log.d("checkLocationPer", "don't show permission again");
                }
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void setUserLocationSettings() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (!fromSearch) {
            setUserLocation();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void setUserLocation() {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        try {
            lat = mLastLocation.getLatitude();
            lng = mLastLocation.getLongitude();
        } catch (NullPointerException e) {
            Log.e("getUserLatLng", "Couldn't get lat or long from last location: " + e.toString());
        }
    }

    @SuppressWarnings("MissingPermission")
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
                    if (isGoogleConnected) {
                        setUserLocationSettings();
                    }
                } else {
                    Log.d("permissionsResult", "fine location not granted");
                    mMap.setMyLocationEnabled(false);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void createCustomMarker(LatLng placeLatLng) {
        if (mMap != null) {
            MarkerOptions marker = new MarkerOptions().position(placeLatLng)
                    .title("custom")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mMap.addMarker(marker);
        }
    }

    // http://stackoverflow.com/a/10407371/1860436
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(getContext(), data);
                LatLng placeLatLng = place.getLatLng();
                setLatLng(placeLatLng);
                moveCamera(mMap, placeLatLng);
                createNearbyMarkers(mMap);
                createCustomMarker(placeLatLng);
                fromSearch = true;
                Log.i("onActivityResult", "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getContext(), data);
                Log.i("onActivityResult", status.getStatusMessage());
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // The user canceled the operation.
                Log.i("onActivityResult", "result cancelled");
            }
        } else if (requestCode == FORM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Snackbar.make(coordinatorLayout, "Places data submitted successfully", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "APIClient isGoogleConnected");
        isGoogleConnected = true;
        if (!isMapSetup) {
            setUpMap();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
            if (!placesRequestSubmitted) {
                placesRequestSubmitted = true;
                setUpMap();
            }
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
        isGoogleConnected = false;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapView != null) {
            mMapView.onLowMemory();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        isGoogleConnected = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("setUserVisibleHint", "PlacesFragment is visible.");
            isVisible = true;
            if (!placesRequestSubmitted) {
                placesRequestSubmitted = true;
                setUpMap();
            }
        }
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

    private long getKeyValue(Map<String, Object> dietary, String key) {
        long value = 0;
        try {
            value = (long) dietary.get(key);
        } catch (Exception e) {
            Log.e("getKeyValue", "Issue getting " + key + " from dietary requirement");
        }
        return value;
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

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        if (Utilities.isPortraitMode(getContext())) {
            inflater.inflate(R.menu.menu, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (Utilities.isPortraitMode(getContext())) {
            menu.findItem(R.id.action_search).setVisible(false);
            super.onPrepareOptionsMenu(menu);
        }
    }

    private LatLng getLatLng() {
        return new LatLng(lat, lng);
    }

    private void setLatLng(LatLng latLng) {
        lat = latLng.latitude;
        lng = latLng.longitude;
    }
}
