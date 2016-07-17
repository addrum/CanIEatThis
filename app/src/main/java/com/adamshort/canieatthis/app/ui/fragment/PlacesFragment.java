package com.adamshort.canieatthis.app.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageView;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.Installation;
import com.adamshort.canieatthis.app.ui.PopupAdapter;
import com.adamshort.canieatthis.app.util.PreferencesHelper;
import com.adamshort.canieatthis.app.util.QueryURLAsync;
import com.adamshort.canieatthis.app.util.Utilities;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlacesFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {
    private static final int ADD_PLACES_INFO_DIALOG_FRAGMENT = 4;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final float MY_LOCATION_ZOOM = 15;

    private static boolean mFromSearch;

    private static String mRadius = "1000";
    private static String mNextPageToken;

    private boolean mIsGoogleConnected;
    private boolean mIsMapSetup;
    private boolean mIsVisible;
    private boolean mPlacesRequestSubmitted;
    private double mLat;
    private double mLng;
    private float mMapZoom = 15;
    private String mApiKey;

    private ImageView mMyLocationButton;
    private Button mShowMoreButton;
    private CoordinatorLayout mCoordinatorLayout;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private MapView mMapView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate and return the layout
        View v = inflater.inflate(R.layout.fragment_places, container, false);
        setHasOptionsMenu(true);
        mApiKey = getResources().getString(R.string.server_key);

        mCoordinatorLayout = (CoordinatorLayout) v.findViewById(R.id.places_coordinator_layout);

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.onCreate(mapViewSavedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately
        mMapView.getMapAsync(this);

        mMyLocationButton = (ImageView) v.findViewById(R.id.myLocationButton);
        mMyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUserLocation();
                moveCamera(mMap, getLatLng(), MY_LOCATION_ZOOM);
                createNearbyMarkers(mMap);
                mFromSearch = false;
            }
        });

        mShowMoreButton = (Button) v.findViewById(R.id.showMoreButton);
        mShowMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMore(mNextPageToken);
                mShowMoreButton.setVisibility(View.INVISIBLE);
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

    /**
     * Creates a Google API Client which is needed for using the Places API.
     */
    private void createGoogleAPIClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
            mIsGoogleConnected = true;
        }
    }

    /**
     * Moves the camera to a specified location.
     *
     * @param googleMap The map to move the camera of.
     * @param latLng    The position to move the camera too.
     */
    private void moveCamera(GoogleMap googleMap, LatLng latLng, float zoom) {
        if ((mIsVisible || !Utilities.isPortraitMode(getContext())) && googleMap != null && latLng != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng).zoom(zoom).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            Log.d("moveCamera", "Moving camera to: " + cameraPosition);
        }
    }

    /**
     * Sets all the properties of the map view, including:
     * - moving camera to either the last location or the users current location
     * - creates any nearby markers
     * - creates a marker at the last location if the user used the search function
     */
    private void setUpMap() {
        createGoogleAPIClient();
        if (mMap != null) {
            if (mIsVisible || !Utilities.isPortraitMode(getContext())) {
                checkLocationPermission();
                LatLng latLng = getLatLng();
                if (latLng.latitude != 0 && latLng.longitude != 0) {
                    moveCamera(mMap, getLatLng(), mMapZoom);

                    createNearbyMarkers(mMap);
                    if (mFromSearch) {
                        createCustomMarker(getLatLng());
                    }
                }
                mIsMapSetup = true;
            }
        }
        mPlacesRequestSubmitted = false;
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d("onMapReady", "Map is ready");

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mMapZoom = cameraPosition.zoom;
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
                        mShowMoreButton.setVisibility(View.INVISIBLE);
                        AddPlacesInfoDialogFragment dialog = new AddPlacesInfoDialogFragment();
                        Bundle args = new Bundle();
                        args.putString("name", marker.getTitle());
                        args.putString("latlng", marker.getPosition().toString());
                        dialog.setArguments(args);
                        // http://stackoverflow.com/a/13733914/1860436
                        dialog.setTargetFragment(PlacesFragment.this, ADD_PLACES_INFO_DIALOG_FRAGMENT);
                        dialog.show(getFragmentManager().beginTransaction(), "AddPlacesInfoDialog");
                    } else {
                        Snackbar.make(mCoordinatorLayout, "You have already submitted information about this place"
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

        mMap = googleMap;
    }

    /**
     * Creates markers near to the latlng position by making an async Places API request.
     *
     * @param googleMap The map to create the markers on.
     */
    private void createNearbyMarkers(GoogleMap googleMap) {
        if (mIsGoogleConnected) {
            if (googleMap != null) {
                googleMap.clear();
            }
            if (mLat != 0 && mLng != 0) {
                String url = getString(R.string.placesUrl) + mLat + "," + mLng + "&radius=" + mRadius + "&type=restaurant&key=" + mApiKey;
                queryPlacesURL(url);
            } else {
                Log.d("createNearbyMarkers", "mLat mLng were 0");
            }
        } else {
            Log.d("createNearbyMarkers", "Not connected so can't make places request");
        }
    }

    /**
     * Creates a singular marker at a specified location taken from the place's JSONObject.
     *
     * @param location JSONObject of information of a location.
     */
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

    /**
     * Shows more markers from a paginated Places API query. Shows up to 60 markers (limit by Google).
     *
     * @param nextPageToken The token needed to get the next set of markers.
     */
    private void showMore(String nextPageToken) {
        if (!nextPageToken.equals("")) {
            String url = getString(R.string.placesUrl) + mLat + "," + mLng + "&mRadius=" + mRadius + "&type=restaurant&key=" + mApiKey
                    + "&pagetoken=" + nextPageToken;
            queryPlacesURL(url);
        }
        Log.d("showMore", "Next page token was null, won't show more");
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
                try {
                    JSONObject response = new JSONObject(output);
                    JSONArray results = response.getJSONArray("results");
                    try {
                        PlacesFragment.mNextPageToken = response.getString("next_page_token");
                        if (!mNextPageToken.equals("")) {
                            mShowMoreButton.setVisibility(View.VISIBLE);
                        }
                    } catch (JSONException e) {
                        mNextPageToken = "";
                        mShowMoreButton.setVisibility(View.INVISIBLE);
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

    /**
     * Checks if the user has provided permission to use their location. Limits to asking twice.
     * After the first ask, shows a snackbar indefinitely which shows the permissions dialog again.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("checkLocationPref", "permission already granted");
            mMyLocationButton.setVisibility(View.VISIBLE);
            setUserLocationSettings();
        } else {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("checkLocationPer", "should show request permission rationale");
                // Need to show permission rationale, display a snackbar and then request
                // the permission again when the snackbar is dismissed.
                Snackbar.make(mCoordinatorLayout,
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
                int timesAsked = PreferencesHelper.getTimesAskedForPermPref(getContext());
                if (timesAsked < 2) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSION_ACCESS_FINE_LOCATION);
                    timesAsked += 1;
                    PreferencesHelper.setTimesAskedForPermPref(getContext(), timesAsked);
                } else {
                    Log.d("checkLocationPer", "don't show permission again");
                }
            }
            mMyLocationButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Shows the user location button and sets latlng to the users location if a search has NOT been performed.
     */
    @SuppressWarnings("MissingPermission")
    private void setUserLocationSettings() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (!mFromSearch) {
            setUserLocation();
        }
    }

    /**
     * Sets the users last location.
     */
    @SuppressWarnings("MissingPermission")
    private void setUserLocation() {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        try {
            mLat = mLastLocation.getLatitude();
            mLng = mLastLocation.getLongitude();
        } catch (NullPointerException e) {
            Log.e("getUserLatLng", "Couldn't get mLat or long from last location: " + e.toString());
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
                    if (mIsGoogleConnected) {
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

    /**
     * Creates a marker at the specified location. Used when returning from a search.
     *
     * @param placeLatLng The location to create a marker at.
     */
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
                moveCamera(mMap, placeLatLng, mMapZoom);
                createNearbyMarkers(mMap);
                createCustomMarker(placeLatLng);
                mFromSearch = true;
                Log.i("onActivityResult", "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getContext(), data);
                Log.i("onActivityResult", status.getStatusMessage());
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // The user canceled the operation.
                Log.i("onActivityResult", "result cancelled");
            }
        } else if (requestCode == ADD_PLACES_INFO_DIALOG_FRAGMENT) {
            if (resultCode == Activity.RESULT_OK) {
                Snackbar.make(mCoordinatorLayout, R.string.placesDataSubmitted, Snackbar.LENGTH_LONG).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Snackbar.make(mCoordinatorLayout, R.string.placesSubmittingError, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "APIClient mIsGoogleConnected");
        mIsGoogleConnected = true;
        if (!mIsMapSetup) {
            setUpMap();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
            if (!mPlacesRequestSubmitted) {
                mPlacesRequestSubmitted = true;
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
            mMapZoom = mMap.getCameraPosition().zoom;
        }
        mMyLocationButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        if (mMap != null) {
            mMap.clear();
            mMapZoom = mMap.getCameraPosition().zoom;
        }
        mIsGoogleConnected = false;
        mMyLocationButton.setVisibility(View.INVISIBLE);
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
        mIsGoogleConnected = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("setUserVisibleHint", "PlacesFragment is visible.");
            Log.d("setUserVisibleHint", "mMapZoom: " + mMapZoom);
            mIsVisible = true;
            if (!mPlacesRequestSubmitted) {
                mPlacesRequestSubmitted = true;
                setUpMap();
            }
        } else {
            if (mMap != null) {
                mMapZoom = mMap.getCameraPosition().zoom;
            }
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
            Log.e("getKeyValue", "Issue getting " + key + " from dietary requirement");
        }
        return value;
    }

    /**
     * Queries firebase asynchronously and adds information about the place to the custom info marker.
     */
    private class FirebaseAsyncRequest extends AsyncTask<MarkerOptions, Void, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(MarkerOptions... params) {
            final MarkerOptions marker = params[0];
            Firebase ref = new Firebase(getString(R.string.firebase_url) + "/places");
            ref.keepSynced(true);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressWarnings("unchecked")
                @Override
                public void onDataChange(DataSnapshot snapshot) {
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
                                        Matcher m = lactosePattern.matcher(snippet);
                                        StringBuffer sb = new StringBuffer();
                                        if (m.find()) {
                                            if (lactose_true > lactose_false) {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "Yes"));
                                            } else {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "No"));
                                            }
                                            snippet = sb.toString();
                                        } else {
                                            snippet += ",Lactose Free: ";
                                            if (lactose_true > lactose_false) {
                                                snippet += "Yes";
                                            } else {
                                                snippet += "No";
                                            }
                                        }
                                    }
                                    bools[0] = lactose_true > lactose_false;

                                    Map<String, Object> vegetarian = loc.get("vegetarian");
                                    long vegetarian_true = getKeyValue(vegetarian, "true");
                                    long vegetarian_false = getKeyValue(vegetarian, "false");
                                    if (shouldShowInfo(vegetarian_true, vegetarian_false)) {
                                        Matcher m = vegetarianPattern.matcher(snippet);
                                        StringBuffer sb = new StringBuffer();
                                        if (m.find()) {
                                            if (vegetarian_true > vegetarian_false) {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "Yes"));
                                            } else {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "No"));
                                            }
                                            snippet = sb.toString();
                                        } else {
                                            snippet += ",Vegetarian: ";
                                            if (vegetarian_true > vegetarian_false) {
                                                snippet += "Yes";
                                            } else {
                                                snippet += "No";
                                            }
                                        }
                                    }
                                    bools[1] = vegetarian_true > vegetarian_false;

                                    Map<String, Object> vegan = loc.get("vegan");
                                    long vegan_true = getKeyValue(vegan, "true");
                                    long vegan_false = getKeyValue(vegan, "false");
                                    if (shouldShowInfo(vegan_true, vegan_false)) {
                                        Matcher m = veganPattern.matcher(snippet);
                                        StringBuffer sb = new StringBuffer();
                                        if (m.find()) {
                                            if (vegan_true > vegan_false) {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "Yes"));
                                            } else {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "No"));
                                            }
                                            snippet = sb.toString();
                                        } else {
                                            snippet += ",Vegan: ";
                                            if (vegan_true > vegan_false) {
                                                snippet += "Yes";
                                            } else {
                                                snippet += "No";
                                            }
                                        }
                                    }
                                    bools[2] = vegan_true > vegan_false;

                                    Map<String, Object> gluten = loc.get("gluten_free");
                                    long gluten_true = getKeyValue(gluten, "true");
                                    long gluten_false = getKeyValue(gluten, "false");
                                    if (shouldShowInfo(gluten_true, gluten_false)) {
                                        Matcher m = glutenPattern.matcher(snippet);
                                        StringBuffer sb = new StringBuffer();
                                        if (m.find()) {
                                            if (gluten_true > gluten_false) {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "Yes"));
                                            } else {
                                                m.appendReplacement(sb, snippet.replace(m.group(1), "No"));
                                            }
                                            snippet = sb.toString();
                                        } else {
                                            snippet += ",Gluten Free: ";
                                            if (gluten_true > gluten_false) {
                                                snippet += "Yes";
                                            } else {
                                                snippet += "No";
                                            }
                                        }
                                    }
                                    bools[3] = gluten_true > gluten_false;
                                } catch (Exception e) {
                                    Log.e("onDataChange", "Couldn't get key from Map: " + e.toString());
                                }
                            }
                        }
                    }
                    Context context = getContext();
                    boolean lactosePref = PreferencesHelper.getLactoseFreePref(context);
                    boolean vegetarianPref = PreferencesHelper.getVegetarianPref(context);
                    boolean veganPref = PreferencesHelper.getVeganPref(context);
                    boolean glutenPref = PreferencesHelper.getGlutenFreePref(context);
                    Boolean lac = bools[0];
                    Boolean veg = bools[1];
                    Boolean vegan = bools[2];
                    Boolean glu = bools[3];
                    if ((!lactosePref && !vegetarianPref && !veganPref && !glutenPref)
                            || (lactosePref && (lac == null || lac))
                            || (vegetarianPref && (veg == null || veg))
                            || (veganPref && (vegan == null || vegan))
                            || (glutenPref && (glu == null || glu))) {
                        marker.snippet(snippet);
                        mMap.addMarker(marker);
                    } else {
                        Log.d("onDataChange", "Not adding marker. Name: " + marker.getTitle()
                                + " lactosePref: " + lactosePref
                                + " vegetarianPref: " + vegetarianPref
                                + " veganPref: " + veganPref
                                + " glutenPref: " + glutenPref
                                + " lac: " + lac
                                + " veg: " + veg
                                + " vegan: " + vegan
                                + " glu: " + glu);
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
        return new LatLng(mLat, mLng);
    }

    private void setLatLng(LatLng latLng) {
        mLat = latLng.latitude;
        mLng = latLng.longitude;
    }
}
