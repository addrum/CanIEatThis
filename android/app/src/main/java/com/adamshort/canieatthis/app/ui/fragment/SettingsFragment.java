package com.adamshort.canieatthis.app.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.util.Log;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.ui.activity.AppIntroActivity;
import com.adamshort.canieatthis.app.util.Utilities;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        SwitchPreference sp = (SwitchPreference) findPreference(getString(R.string.download_switch_pref_key));
        ListPreference frequencyListPref = (ListPreference) findPreference("frequency_list_pref");
        frequencyListPref.setEnabled(sp.isChecked());

        Preference downloadCSVButton = findPreference(getString(R.string.download_CSV_key));
        downloadCSVButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Utilities.downloadDatabase(getActivity(), WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                return true;
            }
        });

        Preference showIntroButton = findPreference(getString(R.string.show_intro_key));
        showIntroButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d("onCreate", "Showing intro activity");
                Intent intent = new Intent(getActivity(), AppIntroActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            pref.setSummary(lp.getEntry());
        } else if (pref instanceof SwitchPreference) {
            SwitchPreference sp = (SwitchPreference) pref;
            ListPreference frequencyListPref = (ListPreference) findPreference("frequency_list_pref");
            frequencyListPref.setEnabled(sp.isChecked());
        }
    }
}
