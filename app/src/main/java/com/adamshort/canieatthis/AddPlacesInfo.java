package com.adamshort.canieatthis;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;

public class AddPlacesInfo extends AppCompatActivity {

    public static int RESULT_OK = 1;

    private LatLng latLng;
    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_places_info);

        final Context context = getBaseContext();

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.places_coordinator_layout);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String[] latLngStr = intent.getStringExtra("latlng").replace("lat/lng: ", "").replace("(", "")
                .replace(")", "").split(",");
        latLng = new LatLng(Double.parseDouble(latLngStr[0]), Double.parseDouble(latLngStr[1]));

        TextView nameView = (TextView) findViewById(R.id.name_view);
        if (nameView != null) {
            nameView.setText(name);
        }

        final CheckBox dairy_free_checkbox = (CheckBox) findViewById(R.id.lactoseFreeCheckBox);
        final CheckBox vegetarian_checkbox = (CheckBox) findViewById(R.id.vegetarianCheckBox);
        final CheckBox vegan_checkbox = (CheckBox) findViewById(R.id.veganCheckBox);
        final CheckBox gluten_free_checkbox = (CheckBox) findViewById(R.id.glutenFreeCheckBox);

        Button submit = (Button) findViewById(R.id.place_submit_button);
        if (submit != null) {
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                            File file = new File(context.getFilesDir(), Installation.getInstallation());
                            Installation.writeInstallationFile(file, "\n" + latLng.toString(), true);
                        }
                    } catch (Exception e) {
                        Log.e("onClick", e.toString());
                    }
                }
            });
        }
    }

    private void doFirebaseTransaction(Firebase ref) {
        if (ref != null) {
            ref.runTransaction(new Transaction.Handler() {
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

    private class FirebaseAsyncRequest extends AsyncTask<boolean[], Void, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(boolean[]... params) {
            final boolean[] data = params[0];
            String key = (latLng.latitude + " " + latLng.longitude).replace(".", ",");
            String partial = getString(R.string.firebase_url) + "/places/" + key;
            Firebase fRef;

            String lactose = partial + "/lactose_free/";
            fRef = new Firebase(lactose + Boolean.toString(data[0]));
            doFirebaseTransaction(fRef);

            String vegetarian = partial + "/vegetarian/";
            fRef = new Firebase(vegetarian + Boolean.toString(data[1]));
            doFirebaseTransaction(fRef);

            String vegan = partial + "/vegan/";
            fRef = new Firebase(vegan + Boolean.toString(data[2]));
            doFirebaseTransaction(fRef);

            String gluten = partial + "/gluten_free/";
            fRef = new Firebase(gluten + Boolean.toString(data[3]));
            doFirebaseTransaction(fRef);

            return "Successful firebase request";
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null || response.equals("")) {
                Log.d("onPostExecute", "Response was null or empty");
//                Toast.makeText(getBaseContext(), "There was an issue submitting information. Please try again.", Toast.LENGTH_LONG).show();
                Snackbar.make(coordinatorLayout, "There was an issue submitting information. Please try again", Snackbar.LENGTH_LONG).show();
                return;
            }
//            Toast.makeText(getBaseContext(), "Places data submitted successfully", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.putExtra("position", 1);
            intent.putExtra("result", RESULT_OK);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }
}
