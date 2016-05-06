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

import java.util.Iterator;

public class PlacesFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;

    private double latitude;
    private double longitude;

    private MapView mMapView;
    private GoogleApiClient mGoogleApiClient;

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

    private void moveCamera(GoogleMap googleMap, double latitude, double longitude) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        // latitude and longitude
        latitude = 51.5572700;
        longitude = -0.2838810;

        MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitude));
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        googleMap.addMarker(marker);

        moveCamera(googleMap, latitude, longitude);

        if (checkForPermission()) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        String radius = "1000";
        String apiKey = getResources().getString(R.string.server_key);
        String placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + latitude + "," + longitude + "&radius=" + radius + "&type=restaurant&key=" + apiKey;

        RequestHandler rh = new RequestHandler(getActivity().getBaseContext(), null, new RequestHandler.AsyncResponse() {
            @Override
            public void processFinish(String output) {
                try {
                    JSONObject response = new JSONObject(output);
                    JSONArray results = response.getJSONArray("results");

                    for(int i = 0; i < results.length(); i++) {
                        JSONObject location = results.getJSONObject(i).getJSONObject("geometry").getJSONObject("location");
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");

                        LatLng latlng = new LatLng(lat, lng);
                        MarkerOptions marker = new MarkerOptions().position(latlng);
                        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                        googleMap.addMarker(marker);
                        Log.d("DEBUG", "lat " + lat + " lng " + lng);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        rh.execute(placesUrl);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (checkForPermission()) {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
        }
    }

    private boolean checkForPermission() {
        if (ContextCompat.checkSelfPermission(getActivity().getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            Log.d("DEBUG", "Didn't have needed permission, requesting ACCESS_FINE_LOCATION");
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            return false;
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
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
            checkForPermission();
        }
    }
}
