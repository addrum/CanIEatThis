package com.adamshort.canieatthis.wear.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.adamshort.canieatthis.wear.ui.PopupAdapter;
import com.adamshort.canieatthis.wear.util.Utilities;
import com.example.canieatthiswear.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.wearable.Wearable;

public class MainWearActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;

    private boolean mIsGoogleConnected;
    private boolean mIsMapSetup;
    private double mLat;
    private double mLng;
    private float mMapZoom;

    private DismissOverlayView mDismissOverlay;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private MapView mMapView;

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Set the layout. It only contains a SupportMapFragment and a DismissOverlay.
        setContentView(R.layout.activity_main_wear);

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

        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.onCreate(savedState);
        mMapView.onResume(); // needed to get the map to display immediately
        mMapView.getMapAsync(this);

        // Obtain the DismissOverlayView and display the intro help text.
        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText(R.string.wearExitText);
        mDismissOverlay.showIntroIfNecessary();
    }

    /**
     * Creates a Google API Client which is needed for using the Places API.
     */
    private void createGoogleAPIClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                    .addApi(LocationServices.API)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
            mIsGoogleConnected = true;
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
            checkLocationPermission();
            LatLng latLng = getLatLng();
            if (latLng.latitude != 0 && latLng.longitude != 0) {
                moveCamera(mMap, getLatLng(), mMapZoom);
            }
            mIsMapSetup = true;
        }
    }

    /**
     * Moves the camera to a specified location.
     *
     * @param googleMap The map to move the camera of.
     * @param latLng    The position to move the camera too.
     */
    private void moveCamera(GoogleMap googleMap, LatLng latLng, float zoom) {
        if (googleMap != null && latLng != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng).zoom(zoom).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            Log.d("moveCamera", "Moving camera to: " + cameraPosition);
        }
    }

    /**
     * Shows the user location button and sets latlng to the users location if a search has NOT been performed.
     */
    @SuppressWarnings("MissingPermission")
    private void setUserLocationSettings() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        setUserLocation();
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
    public void onMapReady(final GoogleMap googleMap) {
        Log.d("onMapReady", "Map is ready");

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mMapZoom = cameraPosition.zoom;
            }
        });

        googleMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {

            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return marker.getTitle().equals("custom");
            }
        });

        googleMap.setOnMapLongClickListener(this);

        mMap = googleMap;
    }

    /**
     * Checks if the user has provided permission to use their location. Limits to asking twice.
     * After the first ask, shows a snackbar indefinitely which shows the permissions dialog again.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("checkLocationPref", "permission already granted");
            setUserLocationSettings();
        } else {
            int timesAsked = Utilities.getTimesAskedForPermPref(getBaseContext());
            if (timesAsked < 2) {
                ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_ACCESS_FINE_LOCATION);
                timesAsked += 1;
                Utilities.setTimesAskedForPermPref(getBaseContext(), timesAsked);
            } else {
                Log.d("checkLocationPer", "don't show permission again");
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        // Display the dismiss overlay with a button to exit this activity.
        mDismissOverlay.show();
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
            setUpMap();
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
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    private LatLng getLatLng() {
        Log.d("getLatLng", "mLat: " + mLat + " mLng: " + mLng);
        return new LatLng(mLat, mLng);
    }

    private void setLatLng(LatLng latLng) {
        mLat = latLng.latitude;
        mLng = latLng.longitude;
    }
}
