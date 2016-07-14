package com.adamshort.canieatthis.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.data.Installation;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;

public class AddPlacesInfoDialogFragment extends DialogFragment {
    private LatLng mLatLng;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

//        Intent intent = activity.getIntent()Z;
        Bundle args = getArguments();
        String name = args.getString("name");
        //noinspection ConstantConditions
        String[] latLngStr = args.getString("latlng").replace("lat/lng: ", "").replace("(", "")
                .replace(")", "").split(",");
        mLatLng = new LatLng(Double.parseDouble(latLngStr[0]), Double.parseDouble(latLngStr[1]));

        View view = inflater.inflate(R.layout.dialog_fragment_add_places_info, null);

        final CheckBox dairy_free_checkbox = (CheckBox) view.findViewById(R.id.lactoseFreeCheckBox);
        final CheckBox vegetarian_checkbox = (CheckBox) view.findViewById(R.id.vegetarianCheckBox);
        final CheckBox vegan_checkbox = (CheckBox) view.findViewById(R.id.veganCheckBox);
        final CheckBox gluten_free_checkbox = (CheckBox) view.findViewById(R.id.glutenFreeCheckBox);

        builder.setView(inflater.inflate(R.layout.dialog_fragment_add_places_info, null))
                .setTitle(name)
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            if (dairy_free_checkbox != null && vegetarian_checkbox != null &&
                                    vegan_checkbox != null && gluten_free_checkbox != null) {
                                final boolean[] values;

                                values = new boolean[]{dairy_free_checkbox.isChecked(), vegetarian_checkbox.isChecked(),
                                        vegan_checkbox.isChecked(), gluten_free_checkbox.isChecked()};

                                FirebaseAsyncRequest fb = new FirebaseAsyncRequest();
                                fb.execute(values);

                                // write latlng to install file so we know which places an installation
                                // has submitted info for
                                File file = new File(getContext().getFilesDir(), Installation.getInstallation());
                                Installation.writeInstallationFile(file, "\n" + mLatLng.toString(), true);
                                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
                            }
                        } catch (Exception e) {
                            Log.e("onClick", e.toString());
                        }
                    }
                });
        return builder.create();
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

    private class FirebaseAsyncRequest extends AsyncTask<boolean[], Void, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(boolean[]... params) {
            final boolean[] data = params[0];
            String key = (mLatLng.latitude + " " + mLatLng.longitude).replace(".", ",");
            String partial = getString(R.string.firebase_url) + "/places/" + key;
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
    }
}
