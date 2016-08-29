package com.adamshort.canieatthis.app.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.adamshort.canieatthis.R;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.google.android.gms.maps.model.LatLng;

public class SubmitPlacesInfoFirebaseAsync extends AsyncTask<boolean[], Void, String> {

    private Context mContext;

    private LatLng mLatLng;

    public SubmitPlacesInfoFirebaseAsync(Context context, LatLng latLng) {
        mContext = context;
        mLatLng = latLng;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected String doInBackground(boolean[]... params) {
        final boolean[] data = params[0];
        String key = (mLatLng.latitude + " " + mLatLng.longitude).replace(".", ",");
        String partial = mContext.getString(R.string.firebase_url) + "/places/" + key;
        Firebase fRef = new Firebase(partial);

        String lactose = "/lactose_free/" + Boolean.toString(data[0]);
        Log.d("doFirebaseTranscation", "Doing firebase transaction at: " + lactose);
        doFirebaseTransaction(fRef.child(removeUnsupportedFirebaseChars(lactose)));

        String vegetarian = "/vegetarian/" + Boolean.toString(data[1]);
        Log.d("doFirebaseTranscation", "Doing firebase transaction at: " + vegetarian);
        doFirebaseTransaction(fRef.child(removeUnsupportedFirebaseChars(vegetarian)));

        String vegan = "/vegan/" + Boolean.toString(data[2]);
        Log.d("doFirebaseTranscation", "Doing firebase transaction at: " + vegan);
        doFirebaseTransaction(fRef.child(removeUnsupportedFirebaseChars(vegan)));

        String gluten = "/gluten_free/" + Boolean.toString(data[3]);
        Log.d("doFirebaseTranscation", "Doing firebase transaction at: " + gluten);
        doFirebaseTransaction(fRef.child(removeUnsupportedFirebaseChars(gluten)));

        return "Successful firebase request";
    }

    @Override
    protected void onPostExecute(String response) {
    }

    /**
     * Increments a value in firebase.
     *
     * @param fRef The location (URL) to increment at.
     */
    private void doFirebaseTransaction(Firebase fRef) {
        if (fRef != null) {
            fRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() == null) {
                        mutableData.setValue(1);
                    } else {
                        mutableData.setValue((long) mutableData.getValue() + 1);
                    }
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                    if (firebaseError != null) {
                        Log.d("onComplete", "Firebase counter increment failed.");
                    } else {
                        Log.d("onComplete", "Firebase counter increment succeeded.");
                    }
                }
            });
        }
    }

    /**
     * Removes the characters not supported by Firebase.
     *
     * @param s The string to remove characters from.
     * @return The modified string.
     */
    private String removeUnsupportedFirebaseChars(String s) {
        return s.replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "");
    }
}
