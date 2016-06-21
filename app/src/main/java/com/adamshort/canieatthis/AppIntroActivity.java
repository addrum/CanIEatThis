package com.adamshort.canieatthis;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.heinrichreimersoftware.materialintro.slide.Slide;

public class AppIntroActivity extends IntroActivity {

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

        final Slide downloadFrequencySlide = new FragmentSlide.Builder()
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .fragment(DownloadFrequencyFragment.newInstance())
                .canGoForward(true)
                .build();
        addSlide(downloadFrequencySlide);

        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide3Title)
                .description(R.string.slide3Desc)
                .permission(Manifest.permission.ACCESS_FINE_LOCATION)
                .image(R.mipmap.ic_launcher)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());
    }
}
