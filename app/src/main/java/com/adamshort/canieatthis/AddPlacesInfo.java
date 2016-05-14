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
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
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

    private Map<String, Map<String, Object>> setLocationMap(boolean[] data, Map<String, Map<String, Object>> snapshot) {
        try {
            Map<String, Object> lactose = snapshot.get("lactose_free");
            if (data[0]) {
                long lactose_true = (long) lactose.get("true") + 1;
                lactose.put("true", lactose_true);
            } else {
                long lactose_false = (long) lactose.get("false") + 1;
                lactose.put("false", lactose_false);
            }

            Map<String, Object> vegetarian = snapshot.get("vegetarian");
            if (data[1]) {
                long vegetarian_true = (long) vegetarian.get("true") + 1;
                vegetarian.put("true", vegetarian_true);
            } else {
                long vegetarian_false = (long) vegetarian.get("false") + 1;
                vegetarian.put("false", vegetarian_false);
            }

            Map<String, Object> vegan = snapshot.get("vegan");
            if (data[2]) {
                long vegan_true = (long) vegan.get("true") + 1;
                vegan.put("true", vegan_true);
            } else {
                long vegan_false = (long) vegan.get("false") + 1;
                vegan.put("false", vegan_false);
            }

            Map<String, Object> gluten = snapshot.get("gluten_free");
            if (data[3]) {
                long gluten_true = (long) gluten.get("true") + 1;
                gluten.put("true", gluten_true);
            } else {
                long gluten_false = (long) gluten.get("false") + 1;
                gluten.put("false", gluten_false);
            }
        } catch (Exception e) {
            Log.e("setLocationMap", e.toString());
        }

        return snapshot;
    }

    private Map<String, Map<String, Object>> setNewLocationMap(boolean[] data) {
        Map<String, Map<String, Object>> newLocation = new HashMap<>();

        try {
            Map<String, Object> lactose = new HashMap<>();
            if (data[0]) {
                lactose.put("true", (long) 1);
                lactose.put("false", (long) 0);
            } else {
                lactose.put("true", (long) 0);
                lactose.put("false", (long) 1);
            }
            newLocation.put("lactose_free", lactose);

            Map<String, Object> vegetarian = new HashMap<>();
            if (data[1]) {
                vegetarian.put("true", (long) 1);
                vegetarian.put("false", (long) 0);
            } else {
                vegetarian.put("true", (long) 0);
                vegetarian.put("false", (long) 1);
            }
            newLocation.put("vegetarian", vegetarian);

            Map<String, Object> vegan = new HashMap<>();
            if (data[2]) {
                vegan.put("true", (long) 1);
                vegan.put("false", (long) 0);
            } else {
                vegan.put("true", (long) 0);
                vegan.put("false", (long) 1);
            }
            newLocation.put("vegan", vegan);

            Map<String, Object> gluten = new HashMap<>();
            if (data[3]) {
                gluten.put("true", (long) 1);
                gluten.put("false", (long) 0);
            } else {
                gluten.put("true", (long) 0);
                gluten.put("false", (long) 1);
            }
            newLocation.put("gluten_free", gluten);
        } catch (Exception e) {
            Log.e("setLocationMap", e.toString());
        }

        Log.d("setNewLocationMap", newLocation.toString());
        return newLocation;
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
//            final Firebase ref = new Firebase(getString(R.string.firebase_url) + "/places");
//            ref.keepSynced(true);
//            ref.addValueEventListener(new ValueEventListener() {
//                @SuppressWarnings("unchecked")
//                @Override
//                public void onDataChange(DataSnapshot snapshot) {
//                    for (DataSnapshot location : snapshot.getChildren()) {
//                        // Firebase doesn't allow . in key's so had to submit as ,
//                        // so now we need to replace it so we can get it back to latlng
//                        String[] key = location.getKey().replace(",", ".").split(" ");
//                        LatLng locLatLng = null;
//                        try {
//                            locLatLng = new LatLng(Double.parseDouble(key[0]), Double.parseDouble(key[1]));
//                        } catch (NumberFormatException e) {
//                            Log.e("onDataChange", e.toString());
//                        }
//
//                        if (locLatLng != null) {
//                            if (latLng.equals(locLatLng)) {
//                                try {
//                                    ref.child(location.getKey()).setValue(setLocationMap(data,
//                                            (Map<String, Map<String, Object>>) location.getValue()));
//                                    Log.d("onDataChange", "Submitted new info");
//                                } catch (ArrayIndexOutOfBoundsException | NullPointerException | ClassCastException e) {
//                                    Log.d("onDataChange", "Submitted new info");
//                                }
//                                return;
//                            }
//                        }
//                    }
//                    try {
//                        ref.child((latLng.latitude + " " + latLng.longitude)
//                                .replace(".", ",")).setValue(setNewLocationMap(data));
//                    } catch (ArrayIndexOutOfBoundsException e) {
//                        Log.e("onDataChange", e.toString());
//                    }
//                }
//
//                @Override
//                public void onCancelled(FirebaseError error) {
//                }
//            });

//            try {
//                ref.child(key).setValue(setLocationMap(data, (Map<String, Map<String, Object>>) key.getValue()));
//                Log.d("onDataChange", "Submitted new info");
//            } catch (ArrayIndexOutOfBoundsException | NullPointerException | ClassCastException e) {
//                Log.d("onDataChange", "Submitted new info");
//            }
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
