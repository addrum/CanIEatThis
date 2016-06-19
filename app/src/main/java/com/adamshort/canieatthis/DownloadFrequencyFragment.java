package com.adamshort.canieatthis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.heinrichreimersoftware.materialintro.app.SlideFragment;

public class DownloadFrequencyFragment extends SlideFragment {

    public DownloadFrequencyFragment() {
        // Required empty public constructor
    }

    public static DownloadFrequencyFragment newInstance() {
        return new DownloadFrequencyFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_download_frequency, container, false);

        final Spinner updateFrequencySpinner = (Spinner) root.findViewById(R.id.updateFrequencySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.updateCheckFrequencyArray, android.R.layout.simple_spinner_dropdown_item);
        if (updateFrequencySpinner != null) {
            updateFrequencySpinner.setAdapter(adapter);
        }

        // set default to checking daily for updates
        Utilities.setDownloadSwitchPref(getContext(), true);
        Utilities.setFrequencyListPref(getContext(), "0");

        if (updateFrequencySpinner != null) {
            updateFrequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    Utilities.setFrequencyListPref(getContext(), Long.toString(id));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }

            });
        }

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}