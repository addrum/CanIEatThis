package com.adamshort.canieatthis;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Element versionElement = new Element();
        versionElement.setTitle("Version 1.0.1");

        Element gitHubElement = new Element();
        String issuesUrl = "https://github.com/addrum/CanIEatThis-Public-/issues";
        Intent ghIntent = new Intent(Intent.ACTION_VIEW);
        ghIntent.setData(Uri.parse(issuesUrl));
        gitHubElement.setTitle("Report an Issue")
                .setIntent(ghIntent)
                .setIcon(R.drawable.about_icon_github);

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setDescription(getString(R.string.aboutLegal))
                .addItem(versionElement)
                .addGroup("Connect with us")
                .addEmail(getString(R.string.aboutEmail))
                .addPlayStore(getString(R.string.packageName))
                .addItem(gitHubElement)
                .create();

        setContentView(aboutPage);
    }
}
