package com.adamshort.canieatthis.app.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.DataPasser;
import com.adamshort.canieatthis.app.data.Installation;
import com.adamshort.canieatthis.app.data.PlacesHelper;
import com.adamshort.canieatthis.app.ui.PopupAdapter;
import com.adamshort.canieatthis.app.util.NextPageListener;
import com.adamshort.canieatthis.app.util.PreferencesHelper;
import com.adamshort.canieatthis.app.util.Utilities;
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
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlacesFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        OnMapReadyCallback,
        NextPageListener {

    private static final int ADD_PLACES_INFO_DIALOG_FRAGMENT = 0;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;
    private static final float MY_LOCATION_ZOOM = 15;

    private static String mRadius = "1000";

    private boolean mIsMapSetup;
    private boolean mIsVisible;
    private boolean mPlacesRequestSubmitted;
    private double mLat;
    private double mLng;
    private float mMapZoom = 15;
    private String mApiKey;

    private BroadcastReceiver mBroadcastReceiver;
    private Button mSearchButton;
    private Button mShowMoreButton;
    private CoordinatorLayout mCoordinatorLayout;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private ImageView mMyLocationButton;
    private MapView mMapView;
    private PlacesHelper mPlacesHelper;
    private TextView mOfflineTextView;

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
        MapsInitializer.initialize(getContext());

        mMyLocationButton = (ImageView) v.findViewById(R.id.myLocationButton);
        mMyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataPasser.getInstance(getContext());
                double lat = mLat;
                double lng = mLng;
                setUserLocation();
                moveCamera(mMap, getLatLng(), MY_LOCATION_ZOOM);
                if (locationIsMoreThan2MetersAway(lat, lng)) {
                    createNearbyMarkers(mMap);
                }
                PreferencesHelper.setFromSearchPref(getContext(), false);
            }
        });

        mShowMoreButton = (Button) v.findViewById(R.id.showMoreButton);
        mShowMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMore(mPlacesHelper.getNextPageToken());
                mShowMoreButton.setVisibility(View.INVISIBLE);
            }
        });

        mSearchButton = (Button) v.findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
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

        mOfflineTextView = (TextView) v.findViewById(R.id.offlineTextView);

        if (!Utilities.isPortraitMode(getContext())) {
            mIsVisible = true;
        }

        LatLng dPLatLng = DataPasser.getLatLng();
        if (dPLatLng != null) {
            setLatLng(dPLatLng);
        }

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
                    .addApiIfAvailable(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    /**
     * Moves the camera to a specified location.
     *
     * @param googleMap The map to move the camera of.
     * @param latLng    The position to move the camera to.
     * @param zoom      Zoom level to move the camera to.
     */
    private void moveCamera(GoogleMap googleMap, LatLng latLng, float zoom) {
        if (mIsVisible && googleMap != null && latLng != null) {
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
            if (mIsVisible) {
                if (isAccessFineLocationPermissionGranted()) {
                    Log.d("locationPermGranted", "location permission already granted");
                    setUserLocationSettings();
                    LatLng latLng = getLatLng();
                    if (latLng.latitude != 0 && latLng.longitude != 0) {
                        createMarkers();
                        mIsMapSetup = true;
                    }
                } else {
                    requestFineLocationPermission();
                }
            }
            mPlacesRequestSubmitted = false;
        }
    }

    public void createMarkers() {
        DataPasser.getInstance(getContext());
        List<MarkerOptions> markersList = DataPasser.getMarkersList();
        if (markersList != null && markersList.size() != 0) {
            Log.i("setUpMap", "Creating markers from DataPasser, size is: " + markersList.size());
            for (MarkerOptions marker : markersList) {
                mPlacesHelper.createMarker(null, marker);
            }
        } else {
            if (!PreferencesHelper.getFromSearchPref(getContext())) {
                createNearbyMarkers(mMap);
            }
        }
        if (!PreferencesHelper.getFromSearchPref(getContext())) {
            moveCamera(mMap, getLatLng(), mMapZoom);
        } else {
            Log.i("setUpMap", "fromSearch pref was true so only creating custom marker");
            createCustomMarker(getLatLng());
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.i("onMapReady", "Map is ready");

        googleMap.setMinZoomPreference(10);

        googleMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater(getArguments())));

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                try {
                    Installation.id(getContext());
                    if (!Installation.isInInstallationFile(getContext(),
                            marker.getPosition().toString())) {
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
                Log.d("onMarkerClick", "Marker: " + marker.getTitle() + "LatLng: " + marker.getPosition());
                return marker.getTitle().equals("custom");
            }
        });

        mPlacesHelper = new PlacesHelper(getContext(), googleMap, false);
        mPlacesHelper.addNextPageListener(this);

        mMap = googleMap;
    }

    /**
     * Creates markers near to the latlng position by making an async Places API request.
     *
     * @param googleMap The map to create the markers on.
     */

    private void createNearbyMarkers(GoogleMap googleMap) {
        if (mGoogleApiClient.isConnected()) {
            if (googleMap != null) {
                googleMap.clear();
                Log.d("onClick", "Setting markers list to empty");
                DataPasser.getInstance(getContext());
                DataPasser.setMarkersList(new ArrayList<MarkerOptions>());
            }
            if (mLat != 0 && mLng != 0) {
                String url = getString(R.string.placesUrl) + mLat + "," + mLng + "&radius=" + mRadius + "&type=restaurant&key=" + mApiKey;
                mPlacesHelper.doPlacesAPIRequest(url, mLat, mLng);
            } else {
                Log.d("createNearbyMarkers", "mLat mLng were 0");
            }
        } else {
            Log.i("createNearbyMarkers", "Not connected so can't make places request");
        }
    }

    /**
     * Shows more markers from a paginated Places API query. Shows up to 60 markers (limit by Google).
     *
     * @param nextPageToken The token needed to get the next set of markers.
     */
    private void showMore(String nextPageToken) {
        if (!TextUtils.isEmpty(nextPageToken)) {
            String url = getString(R.string.placesUrl) + mLat + "," + mLng + "&mRadius=" + mRadius + "&type=restaurant&key=" + mApiKey
                    + "&pagetoken=" + nextPageToken;
            mPlacesHelper.doPlacesAPIRequest(url, mLat, mLng);
        }
        Log.d("showMore", "Next page token was null, won't show more");
    }

    private boolean isAccessFineLocationPermissionGranted() {
        return PreferencesHelper.getIntroShownPref(getContext()) && ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if the user has provided permission to use their location. Limits to asking twice.
     * After the first ask, shows a snackbar indefinitely which shows the permissions dialog again.
     */
    private void requestFineLocationPermission() {
        if (PreferencesHelper.getIntroShownPref(getContext())) {
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
                    showLocationPermissionSnackbar();
                }
            }
            mMyLocationButton.setVisibility(View.INVISIBLE);
        }
    }

    private void showLocationPermissionSnackbar() {
        Snackbar.make(mCoordinatorLayout,
                R.string.fineLocationRationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction("Go to Settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent();
                        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.addCategory(Intent.CATEGORY_DEFAULT);
                        i.setData(Uri.parse("package:" + getContext().getPackageName()));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        getContext().startActivity(i);
                    }
                })
                .show();
    }

    /**
     * Shows the user location button and sets latlng to the users location if a search has NOT been performed.
     */
    @SuppressWarnings("MissingPermission")
    private void setUserLocationSettings() {
        mMyLocationButton.setVisibility(View.VISIBLE);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (!PreferencesHelper.getFromSearchPref(getContext())) {
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
            Log.w("getUserLatLng", "Couldn't get mLat or long from last location: " + e.toString());
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
                    if (mGoogleApiClient.isConnected()) {
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

    private boolean locationIsMoreThan2MetersAway(double lat, double lng) {
        Location loc = new Location("");
        loc.setLatitude(lat);
        loc.setLongitude(lng);

        Location user = new Location("");
        user.setLatitude(mLat);
        user.setLongitude(mLng);
        float distanceInMeters = loc.distanceTo(user);
        return distanceInMeters > 2;
    }

    // http://stackoverflow.com/a/10407371/1860436
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(getContext(), data);
                LatLng placeLatLng = place.getLatLng();
                double lat = mLat;
                double lng = mLng;
                setLatLng(placeLatLng);
                if (locationIsMoreThan2MetersAway(lat, lng)) {
                    moveCamera(mMap, placeLatLng, mMapZoom);
                    createNearbyMarkers(mMap);
                    createCustomMarker(placeLatLng);
                }
                PreferencesHelper.setFromSearchPref(getContext(), true);
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
        Log.i("onConnected", "APIClient mIsGoogleConnected");
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
        registerBroadcastReceiver();
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
        mMyLocationButton.setVisibility(View.INVISIBLE);

        if (mBroadcastReceiver != null) {
            getContext().unregisterReceiver(mBroadcastReceiver);
        }
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

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if ((Utilities.isPortraitMode(getContext()) && !Utilities.isLargeDevice(getContext()))
                || !Utilities.isPortraitMode(getContext()) && !Utilities.isLargeDevice(getContext())
                || Utilities.isPortraitMode(getContext()) && Utilities.isLargeDevice(getContext())) {
            inflater.inflate(R.menu.menu, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if ((Utilities.isPortraitMode(getContext()) && !Utilities.isLargeDevice(getContext()))
                || !Utilities.isPortraitMode(getContext()) && !Utilities.isLargeDevice(getContext())
                || Utilities.isPortraitMode(getContext()) && Utilities.isLargeDevice(getContext())) {
            menu.findItem(R.id.action_search).setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    private void shouldShowMapItems(boolean visibility) {
        int shouldShow;
        if (visibility) {
            shouldShow = View.VISIBLE;
            shouldShowOfflineTextView(View.INVISIBLE);
        } else {
            shouldShow = View.INVISIBLE;
            shouldShowOfflineTextView(View.VISIBLE);
        }
        mSearchButton.setVisibility(shouldShow);
        mShowMoreButton.setVisibility(shouldShow);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMyLocationButton.setVisibility(shouldShow);
        }
    }

    private void shouldShowOfflineTextView(int visibility) {
        mOfflineTextView.setVisibility(visibility);
    }

    private LatLng getLatLng() {
        return new LatLng(mLat, mLng);
    }

    private void setLatLng(LatLng latLng) {
        mLat = latLng.latitude;
        mLng = latLng.longitude;
        DataPasser.setLatLng(latLng);
    }

    private void registerBroadcastReceiver() {
        Log.i("registerBroadcastRec", "Registering broadcast receiver on PlacesFragment");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i("onReceive", "Received a change in the broadcast receiver");
                    shouldShowMapItems(Utilities.hasInternetConnection(context));
                }
            };
        }
        getContext().registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void showMore(int visibility) {
        mShowMoreButton.setVisibility(visibility);
    }
}
