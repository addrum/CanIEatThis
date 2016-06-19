package com.adamshort.canieatthis;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

public class AppIntroActivity extends IntroActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(new SimpleSlide.Builder()
                .title("CanIEatThis")
                .description("Welcome!")
                .image(R.mipmap.ic_launcher)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Download Offline Database")
                .description("To enable offline scanning of products, we need to download the database. " +
                        "Press the button below to do that now! (You will see a download notification appear.")
                .buttonCtaLabel("Download Database")
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

        addSlide(new SimpleSlide.Builder()
                .title("Finding Restaurants Near You")
                .description("In order to find restaurants, we need access to your location. " +
                        "Press the button below to do that now!")
                .permission(Manifest.permission.ACCESS_FINE_LOCATION)
                .image(R.mipmap.ic_launcher)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());
    }
}
