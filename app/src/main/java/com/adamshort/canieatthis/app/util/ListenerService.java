package com.adamshort.canieatthis.app.util;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.PlacesHelper;
import com.adamshort.canieatthis.app.ui.activity.AddInfoFromWearActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class ListenerService extends WearableListenerService implements NextPageListener {

    private PlacesHelper mPlacesHelper;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        final String message = new String(messageEvent.getData());
        Log.d("onMessageReceived", "Message path received on phone is: " + messageEvent.getPath());

        switch (path) {
            case "/add_places_info_phone_path":
                // Broadcast message to wearable activity for display
                Intent intent = new Intent(this, AddInfoFromWearActivity.class);
                intent.putExtra("message_from_watch", message);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case "/request_markers":
                doPlacesAPIRequest(message, null);
                break;
            case "/show_more":
                doPlacesAPIRequest(message, mPlacesHelper.getNextPageToken());
                break;
            default:
                super.onMessageReceived(messageEvent);
                break;
        }
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

        if (mPlacesHelper == null) {
            mPlacesHelper = new PlacesHelper(getApplicationContext(), null, true);
            mPlacesHelper.addNextPageListener(this);
        }
        mPlacesHelper.doPlacesAPIRequest(url, latitude, longitude);
    }

    @Override
    public void showMore(int visibility) {
        new SendToDataLayerThread("/watch_path", Integer.toString(visibility), mPlacesHelper.getGoogleApiClient()).start();
    }
}
