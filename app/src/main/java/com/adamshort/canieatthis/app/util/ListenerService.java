package com.adamshort.canieatthis.app.util;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.PlacesHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

public class ListenerService extends WearableListenerService implements NextPageListener {

    private PlacesHelper mPlacesHelper;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        final String message = new String(messageEvent.getData());
        Log.d("onMessageReceived", "Message path received on phone is: " + messageEvent.getPath());

        switch (path) {
            case "/request_markers":
                doPlacesAPIRequest(message, null);
                break;
            case "/show_more":
                doPlacesAPIRequest(message, getPlacesHelper().getNextPageToken());
                break;
            case "/submit_info":
                Log.d("onMessageReceived", message);
                try {
                    JSONObject info = new JSONObject(message);

                    String[] latLngStr = info.getString("latlng").split(",");
                    LatLng latLng = new LatLng(Double.parseDouble(latLngStr[0]), Double.parseDouble(latLngStr[1]));

                    String[] boolsString = info.getString("bools")
                            .replace("[", "")
                            .replace("]", "")
                            .split(",");
                    boolean[] bools = new boolean[boolsString.length];
                    for (int i = 0; i < boolsString.length; i++) {
                        bools[i] = Boolean.parseBoolean(boolsString[i]);
                    }

                    SubmitPlacesInfoFirebaseAsync request = new SubmitPlacesInfoFirebaseAsync(getBaseContext(), latLng);
                    request.execute(bools);
                } catch (JSONException e) {
                    Log.e("onMessageReceived", "Error creating json object for submitting places info " + e);
                }
            default:
                super.onMessageReceived(messageEvent);
                break;
        }
    }

    private PlacesHelper getPlacesHelper() {
        if (mPlacesHelper == null) {
            mPlacesHelper = new PlacesHelper(getApplicationContext(), null, true);
            mPlacesHelper.addNextPageListener(this);
        }
        return mPlacesHelper;
    }

    private void doPlacesAPIRequest(String message, String nextPageToken) {
        String[] latLng = message.split(",");
        double latitude = Double.parseDouble(latLng[0]);
        double longitude = Double.parseDouble(latLng[1]);

        String url = getString(R.string.placesUrl) + latitude + "," + longitude
                + "&radius=" + 1000 + "&type=restaurant&key="
                + getString(R.string.server_key);
        if (!TextUtils.isEmpty(nextPageToken)) {
            url += "&pagetoken=" + nextPageToken;
        }

        getPlacesHelper().doPlacesAPIRequest(url, latitude, longitude);
    }

    @Override
    public void showMore(int visibility) {
        new SendToDataLayerThread("/watch_path", Integer.toString(visibility), getPlacesHelper().getGoogleApiClient()).start();
    }
}
