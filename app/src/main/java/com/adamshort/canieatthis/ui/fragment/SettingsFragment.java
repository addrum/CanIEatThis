package com.adamshort.canieatthis.ui.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;

import com.adamshort.canieatthis.R;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        SwitchPreference sp =(SwitchPreference) findPreference("download_switch_pref");
        ListPreference frequencyListPref = (ListPreference) findPreference("frequency_list_pref");
        frequencyListPref.setEnabled(sp.isChecked());
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
