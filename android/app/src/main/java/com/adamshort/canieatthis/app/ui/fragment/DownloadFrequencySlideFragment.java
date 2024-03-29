package com.adamshort.canieatthis.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.util.PreferencesHelper;
import com.heinrichreimersoftware.materialintro.app.SlideFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloadFrequencySlideFragment extends SlideFragment {

    public DownloadFrequencySlideFragment() {
        // Required empty public constructor
    }

    public static DownloadFrequencySlideFragment newInstance() {
        return new DownloadFrequencySlideFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_download_frequency_slide, container, false);

        final Spinner updateFrequencySpinner = (Spinner) root.findViewById(R.id.updateFrequencySpinner);
        String[] updateCheckFrequencyArray = getResources().getStringArray(R.array.updateCheckFrequencyArray);
        List<String> list;
        list = Arrays.asList(updateCheckFrequencyArray);
        ArrayList<String> arrayList = new ArrayList<>(list);
        arrayList.add("Never");
        updateCheckFrequencyArray = arrayList.toArray(new String[list.size()]);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getContext(), android.R.layout.simple_spinner_dropdown_item, updateCheckFrequencyArray);
        if (updateFrequencySpinner != null) {
            updateFrequencySpinner.setAdapter(adapter);
        }

        // set default to checking daily for updates
        PreferencesHelper.setDownloadSwitchPref(getContext(), true);
        PreferencesHelper.setFrequencyListPref(getContext(), "0");

        if (updateFrequencySpinner != null) {
            updateFrequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if (id == 3) {
                        PreferencesHelper.setDownloadSwitchPref(getContext(), false);
                    } else {
                        PreferencesHelper.setFrequencyListPref(getContext(), Long.toString(id));
                    }
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