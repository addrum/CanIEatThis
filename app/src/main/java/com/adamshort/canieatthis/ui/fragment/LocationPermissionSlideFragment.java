package com.adamshort.canieatthis.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.util.Utilities;
import com.heinrichreimersoftware.materialintro.app.SlideFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationPermissionSlideFragment extends SlideFragment {

    public LocationPermissionSlideFragment() {
        // Required empty public constructor
    }

    public static LocationPermissionSlideFragment newInstance() {
        return new LocationPermissionSlideFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_download_frequency_slide, container, false);
    }

    @Override
    public boolean canGoForward() {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}