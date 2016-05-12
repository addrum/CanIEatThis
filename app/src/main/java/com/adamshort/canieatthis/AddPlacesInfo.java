package com.adamshort.canieatthis;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

public class AddPlacesInfo extends AppCompatActivity {

    private LatLng latLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_places_info);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String[] latLngStr = intent.getStringExtra("latlng").replace("lat/lng: ", "").replace("(", "")
                .replace(")", "").split(",");
        latLng = new LatLng(Double.parseDouble(latLngStr[0]), Double.parseDouble(latLngStr[1]));

        TextView nameView = (TextView) findViewById(R.id.name_view);
        if (nameView != null) {
            nameView.setText(name);
        }

        final CheckBox dairy_free_checkbox = (CheckBox) findViewById(R.id.dairyFreeCheckBox);
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
                        }
                    } catch (Exception e) {
                        Log.e("onClick", e.toString());
                    }
                }
            });
        }
    }

    private Map<String, Object> setLocationMap(boolean[] data) {
        Map<String, Object> values = new HashMap<>();
        values.put("dairy_free", data[0]);
        values.put("vegetarian", data[1]);
        values.put("vegan", data[2]);
        values.put("gluten_free", data[3]);
        return values;
    }

    private class FirebaseAsyncRequest extends AsyncTask<boolean[], Void, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(boolean[]... params) {
            final boolean[] data = params[0];
            final Firebase ref = new Firebase(getString(R.string.firebase_url) + "/places");
            ref.keepSynced(true);
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
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
                            if (latLng.equals(locLatLng)) {
                                try {
                                    ref.child(location.getKey()).setValue(setLocationMap(data));
                                    Log.d("onDataChange", "Submitted new info");
                                } catch (ArrayIndexOutOfBoundsException | NullPointerException | ClassCastException e) {
                                    Log.d("onDataChange", "Submitted new info");
                                }
                                return;
                            }
                        }
                    }
                    try {
                        ref.child((latLng.latitude + " " + latLng.longitude)
                                .replace(".", ",")).setValue(setLocationMap(data));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.e("onDataChange", e.toString());
                    }
                }

                @Override
                public void onCancelled(FirebaseError error) {
                }
            });
            return "Successful firebase request";
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null) {
                Log.d("onPostExecute", "Response was null");
                Toast.makeText(getBaseContext(), "There was an issue submitting information. Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
            if (response.equals("")) {
                Log.d("onPostExecute", "Couldn't post places info");
                Toast.makeText(getBaseContext(), "There was an issue submitting information. Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(getBaseContext(), "Places data submitted successfully", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.putExtra("position", 1);
            startActivity(intent);
        }
    }
}
