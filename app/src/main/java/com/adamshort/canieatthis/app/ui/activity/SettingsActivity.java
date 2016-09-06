package com.adamshort.canieatthis.app.ui.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.adamshort.canieatthis.app.ui.fragment.SettingsFragment;
import com.adamshort.canieatthis.app.util.Utilities;

public class SettingsActivity extends AppCompatActivity {

    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Utilities.downloadDatabase(this, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                }
        }
    }

}
