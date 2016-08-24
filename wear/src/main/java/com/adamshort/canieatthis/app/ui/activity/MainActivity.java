package com.adamshort.canieatthis.app.ui.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.adamshort.canieatthis.app.R;
import com.adamshort.canieatthis.app.ui.PopupAdapter;
import com.adamshort.canieatthis.app.util.PreferencesHelper;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends WearableActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks {

    private final static int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;

    private boolean mIsGoogleConnected;
    private boolean mIsMapSetup;
    private boolean mIsMessageSentToPhone;
    private int mMarkersAdded;
    private double mLat;
    private double mLng;
    private List<MarkerOptions> mMarkers;

    private Button mShowMoreButton;
    private DismissOverlayView mDismissOverlay;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enable ambient support, so the map remains visible in simplified, low-color display
        // when the user is no longer actively using the app but the app is still visible on the
        // watch face.
        setAmbientEnabled();

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

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
        mDismissOverlay.setIntroText(R.string.intro_text);
        mDismissOverlay.showIntroIfNecessary();

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.onCreate(mapViewSavedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately
        mMapView.getMapAsync(this);
        MapsInitializer.initialize(getApplicationContext());

        mShowMoreButton = (Button) findViewById(R.id.showMoreButton);

        mShowMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageToPhone("/show_more", mLat + "," + mLng);
            }
        });

        mMarkers = new ArrayList<>();
    }

    /**
     * Creates a Google API Client which is needed for using the Places API.
     */
    private void createGoogleAPIClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
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
        if (googleMap != null && latLng != null) {
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
            checkLocationPermission();
            LatLng latLng = getLatLng();
            if (latLng.latitude != 0 && latLng.longitude != 0) {
                mIsMapSetup = true;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Log.d("onInfoWindowClick", "Info window clicked");

                try {
                    JSONObject message = new JSONObject();
                    message.put("name", marker.getTitle());
                    message.put("location", marker.getPosition().toString());

                    sendMessageToPhone("/add_places_info_phone_path", message.toString());

                    Toast.makeText(getBaseContext(), "Submit info on your phone", Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    Log.e("onInfoWindowClick", "Issue creating json to send to phone");
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

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                for (MarkerOptions marker : mMarkers) {
                    if (Math.abs(marker.getPosition().latitude - latLng.latitude) < 0.05 && Math.abs(marker.getPosition().longitude - latLng.longitude) < 0.05) {
                        Toast.makeText(getBaseContext(), marker.getTitle() + " got clicked", Toast.LENGTH_SHORT).show(); //do some stuff
                        break;
                    }
                }
            }
        });

        googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                Log.i("onMapReady", "Sending /request_markers to phone");
                clearMap();
                setUserLocation();
                sendMessageToPhone("/request_markers", mLat + "," + mLng);
                return false;
            }
        });

        // Map is ready to be used.
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
            int timesAsked = PreferencesHelper.getTimesAskedForPermPref(getBaseContext());
            if (timesAsked < 2) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_ACCESS_FINE_LOCATION);
                timesAsked += 1;
                PreferencesHelper.setTimesAskedForPermPref(getBaseContext(), timesAsked);
            } else {
                Log.d("checkLocationPer", "don't show permission again");
            }
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
        moveCamera(mMap, new LatLng(mLat, mLng), 15);
    }

    /**
     * Sets the users last location.
     */
    @SuppressWarnings("MissingPermission")
    private void setUserLocation() {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        try {
            Log.i("setUserLocation", "Setting mLat and mLng");
            mLat = mLastLocation.getLatitude();
            mLng = mLastLocation.getLongitude();

            if (!mIsMessageSentToPhone) {
                Log.i("onMapReady", "Sending /request_markers to phone");
                mIsMessageSentToPhone = true;
                clearMap();
                sendMessageToPhone("/request_markers", mLat + "," + mLng);
            }
        } catch (NullPointerException e) {
            Log.w("setUserLocation", "Couldn't get mLat or long from last location: " + e.toString());
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("onConnected", "APIClient mIsGoogleConnected");
        mIsGoogleConnected = true;
        if (!mIsMapSetup) {
            setUpMap();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mIsGoogleConnected = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
            if (!mIsMapSetup) {
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
        mIsGoogleConnected = false;
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

    private LatLng getLatLng() {
        return new LatLng(mLat, mLng);
    }


    /**
     * Creates a singular marker at a specified location taken from the place's JSONObject.
     *
     * @param marker JSONObject of information of a location.
     */
    private void createMarker(JSONObject marker) {
        if (marker != null) {
            try {
                String name = marker.getString("name");

                JSONObject location = marker.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");

                LatLng latlng = new LatLng(lat, lng);
                MarkerOptions newMarker = new MarkerOptions()
                        .position(latlng)
                        .title(name);

                newMarker.snippet(marker.getString("snippet"));
                newMarker.icon(BitmapDescriptorFactory.defaultMarker());

                mMap.addMarker(newMarker);

                mMarkers.add(newMarker);

                Log.d("createMarker", "Name: " + name + " lat " + lat + " lng " + lng);
            } catch (JSONException e) {
                Log.w("createMarker", "Couldn't get info from JSON so not adding marker to map");
            }
        } else {
            Log.w("createMarker", "marker was null");
        }
    }

    private void clearMap() {
        Log.d("clearMap", "Clearing the map, markers added and markers list");
        mMap.clear();
        mMarkersAdded = 0;
        mMarkers = new ArrayList<>();
    }

    private void sendMessageToPhone(String path, String message) {
        Log.i("sendMessageToPhone", "Sending message to phone");
        new SendToDataLayerThread(path, message).start();
    }

    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.i("run", "Message sent to: " + node.getDisplayName());
                    Log.d("run", "Message: " + message);
                } else {
                    // Log an error
                    Log.e("run", "ERROR: failed to send Message");
                }
            }
        }
    }

    class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("onReceive", "Message received in watch activity: " + message);

            if (message.length() == 1) {
                Log.d("onReceive", "showMore visible: " + message);
                //noinspection ResourceType
                mShowMoreButton.setVisibility(Integer.parseInt(message));
            } else {

                // Display message in UI
                try {
                    createMarker(new JSONObject(message));
                } catch (JSONException e) {
                    Log.e("onReceive", "Couldn't create JSON from message");
                }

                if (mMarkersAdded > 19) {
                    Log.d("onReceive", "markers added is greater than 19 so clearing map for performance");
                    mMap.clear();
                    mMarkersAdded = 0;
                }
                mMarkersAdded++;
            }
        }
    }
}
