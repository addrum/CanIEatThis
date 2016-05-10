package com.adamshort.canieatthis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class PlacesFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;

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
                createNearbyMarkers(googleMap);
                return false;
            }
        });

        googleMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater(getArguments())));

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent(getActivity().getBaseContext(), AddPlacesInfo.class);
                intent.putExtra("name", marker.getTitle());
                intent.putExtra("latlng", marker.getPosition().toString());
                startActivity(intent);
            }
        });
    }

    private void createNearbyMarkers(GoogleMap googleMap) {
        googleMap.clear();
        if (checkForPermission()) {
            if (connected) {
                getUserLatLng();
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

    private LatLng getUserLatLng() {
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
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            if (connected && mapReady) {
                moveCamera(mMap, getUserLatLng());
            }
            return false;
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
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "APIClient connected");
        connected = true;
        if (mapReady && mMap != null) {
            moveCamera(mMap, getUserLatLng());
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
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    LatLng markeyLatLng = marker.getPosition();
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
                            if (markeyLatLng.equals(locLatLng)) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> loc = (Map<String, Object>) location.getValue();
                                snippet += ",Dairy Free: ";
                                if ((boolean) loc.get("dairy_free")) {
                                    snippet += "Yes";
                                } else {
                                    snippet += "No";
                                }
                                snippet += ",Vegetarian: ";
                                if ((boolean) loc.get("vegetarian")) {
                                    snippet += "Yes";
                                } else {
                                    snippet += "No";
                                }
                                snippet += ",Vegan: ";
                                if ((boolean) loc.get("vegan")) {
                                    snippet += "Yes";
                                } else {
                                    snippet += "No";
                                }
                                snippet += ",Gluten Free: ";
                                if ((boolean) loc.get("gluten_free")) {
                                    snippet += "Yes";
                                } else {
                                    snippet += "No";
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
}
