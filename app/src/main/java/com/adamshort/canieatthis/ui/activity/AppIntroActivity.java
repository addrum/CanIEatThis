package com.adamshort.canieatthis.ui.activity;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.ui.fragment.DownloadFrequencySlideFragment;
import com.adamshort.canieatthis.ui.fragment.LocationPermissionSlideFragment;
import com.adamshort.canieatthis.util.Utilities;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.app.NavigationPolicy;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

public class AppIntroActivity extends IntroActivity {
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        setNavigationPolicy(new NavigationPolicy() {
            @Override
            public boolean canGoForward(int position) {
                return true;
            }

            @Override
            public boolean canGoBackward(int position) {
                return true;
            }
        });

        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide1Title)
                .description(R.string.slide1Desc)
                .image(R.mipmap.ic_launcher)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide2Title)
                .description(getString(R.string.slide2Desc))
                .buttonCtaLabel(R.string.slide2ButtonLabel)
                .buttonCtaClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utilities.downloadDatabase(AppIntroActivity.this, getBaseContext());
                    }
                })
                .image(R.mipmap.ic_launcher)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());

        addSlide(new FragmentSlide.Builder()
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .fragment(DownloadFrequencySlideFragment.newInstance())
                .build());

        addSlide(new FragmentSlide.Builder()
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .fragment(LocationPermissionSlideFragment.newInstance())
                .buttonCtaLabel(R.string.grantPermissionText)
                .buttonCtaClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
                    }
                })
                .build());
    }
}
