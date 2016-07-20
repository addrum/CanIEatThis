package com.adamshort.canieatthis.app.util;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class ListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/message_path")) {
            final String message = new String(messageEvent.getData());
            Log.i("onMessageReceived", "Message path received on watch is: " + messageEvent.getPath());
            Log.i("onMessageReceived", "Message received on watch is: " + message);
        } else {
            super.onMessageReceived(messageEvent);
        }
    }

}
