package com.adamshort.canieatthis.app.ui.activity;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.adamshort.canieatthis.app.R;
import com.adamshort.canieatthis.app.util.SendToDataLayerThread;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class AddPlacesInfoActivity extends WearableActivity {

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_places_info);

        // Enable ambient support, so the map remains visible in simplified, low-color display
        // when the user is no longer actively using the app but the app is still visible on the
        // watch face.
        setAmbientEnabled();

        createGoogleAPIClient();

        String name = getIntent().getExtras().getString("name");
        final LatLng loc = (LatLng) getIntent().getExtras().get("latlng");

        TextView title = (TextView) findViewById(R.id.title_text_view);
        title.setText(name);

        Button submitButton = (Button) findViewById(R.id.submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CheckBox lactose_free_checkbox = (CheckBox) findViewById(R.id.lactoseFreeCheckBox);
                final CheckBox vegetarian_checkbox = (CheckBox) findViewById(R.id.vegetarianCheckBox);
                final CheckBox vegan_checkbox = (CheckBox) findViewById(R.id.veganCheckBox);
                final CheckBox gluten_free_checkbox = (CheckBox) findViewById(R.id.glutenFreeCheckBox);
                //noinspection ConstantConditions
                boolean[] values = new boolean[]{
                        lactose_free_checkbox.isChecked(),
                        vegetarian_checkbox.isChecked() || vegan_checkbox.isChecked(),
                        vegan_checkbox.isChecked(),
                        gluten_free_checkbox.isChecked()};

                try {
                    JSONObject info = new JSONObject();

                    info.put("latlng", loc.latitude + "," + loc.longitude);

                    JSONArray bools = new JSONArray(Arrays.toString(values));
                    info.put("bools", bools);

                    Log.d("onClick", info.toString());

                    new SendToDataLayerThread("/submit_info", info.toString(), mGoogleApiClient).start();

                    finish();
                } catch (JSONException e) {
                    Log.e("onClick", "Error creating json for submitting info: " + e);
                }

                finish();
            }
        });
    }

    /**
     * Creates a Google API Client which is needed for using the Places API.
     */
    private void createGoogleAPIClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                    .addApi(LocationServices.API)
                    .addApiIfAvailable(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

}
