package com.adamshort.canieatthis.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.util.Utilities;
import com.heinrichreimersoftware.materialintro.app.SlideFragment;

public class UserPreferencesSlideFragment extends SlideFragment {

    public UserPreferencesSlideFragment() {
        // Required empty public constructor
    }

    public static UserPreferencesSlideFragment newInstance() {
        return new UserPreferencesSlideFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_user_preferences_slide, container, false);

        final CheckBox lactoseCheckBox = (CheckBox) root.findViewById(R.id.lactoseFreeSwitch);
        final CheckBox vegetarianCheckBox = (CheckBox) root.findViewById(R.id.vegetarianSwitch);
        final CheckBox veganCheckBox = (CheckBox) root.findViewById(R.id.veganSwitch);
        final CheckBox glutenCheckBox = (CheckBox) root.findViewById(R.id.glutenFreeSwitch);

        lactoseCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Utilities.setLactoseFreePref(getContext(), b);
            }
        });

        vegetarianCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Utilities.setVegetarianPref(getContext(), b);
            }
        });

        veganCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Utilities.setVegeanPref(getContext(), b);
            }
        });

        glutenCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Utilities.setGlutenFreePref(getContext(), b);
            }
        });

        return root;
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