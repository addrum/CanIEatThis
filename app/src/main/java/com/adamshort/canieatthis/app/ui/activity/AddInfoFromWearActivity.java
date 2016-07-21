package com.adamshort.canieatthis.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.Installation;
import com.adamshort.canieatthis.app.ui.fragment.AddPlacesInfoDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AddInfoFromWearActivity extends AppCompatActivity implements AddPlacesInfoDialogFragment.OnCompleteListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_info_from_wear);

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.from_wear_coordinator_layout);

        try {
            JSONObject message = new JSONObject(getIntent().getStringExtra("message_from_watch"));

            String name = message.getString("name");
            String location = message.getString("location");

            try {
                Installation.id(getBaseContext());
                if (!Installation.isInInstallationFile(getBaseContext(), location)) {
                    AddPlacesInfoDialogFragment dialog = new AddPlacesInfoDialogFragment();
                    Bundle args = new Bundle();
                    args.putString("name", name);
                    args.putString("latlng", location);
                    dialog.setArguments(args);
                    // http://stackoverflow.com/a/15121529/1860436
                    dialog.show(getSupportFragmentManager(), "AddPlacesInfoDialog");
                } else {
                    Snackbar.make(coordinatorLayout, "You have already submitted information about this place"
                            , Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Log.e("onInfoWindowClick", "issue checking if latlng is in install file: " + e.toString());
            }
        } catch (JSONException e) {
            Log.e("onCreate", "Couldn't create JSON from message sent from wear");
        }
    }

    @Override
    public void onComplete() {
        Log.i("onComplete", "Finished adding places so returning to main activity");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
