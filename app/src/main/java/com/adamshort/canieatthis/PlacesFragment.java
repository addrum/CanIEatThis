package com.adamshort.canieatthis;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlacesFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;

    private boolean connected;
    private boolean mapReady;
    private double lat;
    private double lng;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private MapView mMapView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflat and return the layout
        View v = inflater.inflate(R.layout.fragment_places, container,
                false);
        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();// needed to get the map to display immediately
        mMapView.getMapAsync(this);

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getBaseContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        // Perform any camera updates here
        return v;
    }

    private void moveCamera(GoogleMap googleMap, LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng).zoom(15).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d("onMapReady", "Map is ready");
        mapReady = true;
        mMap = googleMap;
        if (connected) {
            setUserLatLng();
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
                LatLng old = new LatLng(lat, lng);
                LatLng current = setUserLatLng();
                if (old != current) {
                    createNearbyMarkers(googleMap);
                }
                return false;
            }
        });
    }

    private void createNearbyMarkers(GoogleMap googleMap) {
        final GoogleMap mMap = googleMap;
        if (checkForPermission()) {
            if (connected) {
                setUserLatLng();
            }
            String radius = "1000";
            String apiKey = getResources().getString(R.string.server_key);
            String placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                    + lat + "," + lng + "&radius=" + radius + "&type=restaurant&key=" + apiKey;

            RequestHandler rh = new RequestHandler(getActivity().getBaseContext(), null, new RequestHandler.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    try {
                        JSONObject response = new JSONObject(output);
                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject location = results.getJSONObject(i);
                            String name = location.getString("name");

                            JSONObject geometry = location.getJSONObject("geometry").getJSONObject("location");
                            double lat = geometry.getDouble("lat");
                            double lng = geometry.getDouble("lng");

                            LatLng latlng = new LatLng(lat, lng);
                            MarkerOptions marker = new MarkerOptions().position(latlng)
                                    .title(name);

                            try {
                                boolean openNow = location.getJSONObject("opening_hours").getBoolean("open_now");
                                if (openNow) {
                                    marker.snippet("Open Now: Yes");
                                } else {
                                    marker.snippet("Open Now: No");
                                }
                            } catch (JSONException e) {
                                Log.e("processFinish", "No value for opening_hours or open_now");
                            }

                            marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                            mMap.addMarker(marker);
                            Log.d("DEBUG", "lat " + lat + " lng " + lng);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            rh.execute(placesUrl);
        } else {
            Log.d("createNearbyMarkers", "LatLng was null so won't make places request");
        }
    }

    private LatLng setUserLatLng() {
        if (checkForPermission()) {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            lat = mLastLocation.getLatitude();
            lng = mLastLocation.getLongitude();
            return new LatLng(lat, lng);
        }
        return null;
    }

    private boolean checkForPermission() {
        if (ContextCompat.checkSelfPermission(getActivity().getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            Log.d("checkForPermission", "Didn't have needed permission, requesting ACCESS_FINE_LOCATION");
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            return false;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "APIClient connected");
        setUserLatLng();
        connected = true;
        if (mapReady && mMap != null) {
            moveCamera(mMap, new LatLng(lat, lng));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
        mapReady = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        connected = false;
        mapReady = false;
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
        connected = false;
        mapReady = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            checkForPermission();
        }
    }
}
