package com.adamshort.canieatthis.app.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.ui.fragment.DownloadFrequencySlideFragment;
import com.adamshort.canieatthis.app.ui.fragment.LocationPermissionSlideFragment;
import com.adamshort.canieatthis.app.ui.fragment.UserPreferencesSlideFragment;
import com.adamshort.canieatthis.app.util.Utilities;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

public class AppIntroActivity extends IntroActivity {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                        ActivityCompat.requestPermissions(AppIntroActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
                    }
                })
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.markerInfoExampleTitle)
                .description(R.string.markerExampleDescription)
                .image(R.drawable.marker_view)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());

        addSlide(new FragmentSlide.Builder()
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .fragment(UserPreferencesSlideFragment.newInstance())
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.anyHelpTitle)
                .description(R.string.needHelpDescription)
                .image(R.drawable.help_button)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Utilities.downloadDatabase(this, getBaseContext());
                }
        }
    }
}
