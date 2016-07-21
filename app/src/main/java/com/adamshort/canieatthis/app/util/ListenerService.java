package com.adamshort.canieatthis.app.util;

import android.content.Intent;
import android.util.Log;

import com.adamshort.canieatthis.app.ui.activity.AddInfoFromWearActivity;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class ListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        final String message = new String(messageEvent.getData());
        switch (path) {
            case "/add_places_info_phone_path":
                Log.d("onMessageReceived", "Message path received on phone is: " + messageEvent.getPath());

                // Broadcast message to wearable activity for display
                Intent intent = new Intent(this, AddInfoFromWearActivity.class);
                intent.putExtra("message_from_watch", message);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case "/request_markers":
                Log.d("onMessageReceived", "Message path received on phone is: " + messageEvent.getPath());
                break;
            default:
                super.onMessageReceived(messageEvent);
                break;
        }
    }

}
