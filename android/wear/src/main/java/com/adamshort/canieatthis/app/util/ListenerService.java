package com.adamshort.canieatthis.app.util;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class ListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        final String message = new String(messageEvent.getData());
        Log.d("onMessageReceived", "Message path received on watch is: " + messageEvent.getPath());

        switch (path) {
            case "/watch_path":
                // Broadcast message to wearable activity for display
                Intent messageIntent = new Intent();
                messageIntent.setAction(Intent.ACTION_SEND);
                messageIntent.putExtra("message", message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
                break;
            default:
                super.onMessageReceived(messageEvent);
        }
    }
}
