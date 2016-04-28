package com.adamshort.canieatthis;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        ListPreference updateFrequencyListPref = (ListPreference) findPreference("Update Check Frequency");
        updateFrequencyListPref.setSummary(updateFrequencyListPref.getEntry());
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
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            pref.setSummary(lp.getEntry());
        } else if (pref instanceof SwitchPreference) {
            SwitchPreference sp = (SwitchPreference) pref;
            ListPreference updateListPref = (ListPreference) findPreference("Update Check Frequency");
            updateListPref.setEnabled(sp.isChecked());
            editor.putBoolean("@string/downloadLocalDatabaseSwitchPrefKey", sp.isChecked());
            Log.d("DEBUG", "Should download database: " + sp.isChecked());
        }
        editor.apply();
    }
}
