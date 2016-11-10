package com.adamshort.canieatthis.app.util;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class SendToDataLayerThread extends Thread {

    private String mPath;
    private String mMessage;

    private GoogleApiClient mGoogleApiClient;

    // Constructor to send a message to the data layer
    public SendToDataLayerThread(String path, String msg, GoogleApiClient googleApiClient) {
        mPath = path;
        mMessage = msg;
        mGoogleApiClient = googleApiClient;
    }

    public void run() {
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, node.getId(), mPath, mMessage.getBytes()).await();
            if (result.getStatus().isSuccess()) {
                Log.i("run", "Message sent to: " + node.getDisplayName());
                Log.d("run", "Message: " + mMessage);
            } else {
                // Log an error
                Log.e("run", "ERROR: failed to send Message");
            }
        }
    }
}
