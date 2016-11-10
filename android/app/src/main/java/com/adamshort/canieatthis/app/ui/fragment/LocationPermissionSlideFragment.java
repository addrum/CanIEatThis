package com.adamshort.canieatthis.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.adamshort.canieatthis.R;
import com.heinrichreimersoftware.materialintro.app.SlideFragment;

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
        return inflater.inflate(R.layout.fragment_location_permission_slide, container, false);
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