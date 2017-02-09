package com.adamshort.canieatthis.app.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.adamshort.canieatthis.R;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Element versionElement = new Element();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionElement.setTitle(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("onCreate", "Couldn't get package info: " + e.toString());
        }


        Intent ghIntent = new Intent(Intent.ACTION_VIEW);
        ghIntent.setData(Uri.parse(getString(R.string.issuesUrl)));
        Element gitHubElement = new Element();
        gitHubElement.setTitle("Report an Issue")
                .setIntent(ghIntent)
                .setIcon(R.drawable.about_icon_github);

        Intent offIntent = new Intent(Intent.ACTION_VIEW);
        offIntent.setData(Uri.parse(getString(R.string.offUrl)));
        Element offElement = new Element();
        offElement.setTitle("Open Food Facts")
                .setIntent(offIntent)
                .setIcon(R.drawable.ic_off);

        Intent privacyIntent = new Intent(Intent.ACTION_VIEW);
        privacyIntent.setData(Uri.parse("https://github.com/addrum/CanIEatThis-Public-/blob/master/privacy_policy.md"));
        Element privacyElement = new Element();
        privacyElement.setTitle("Privacy Policy")
                .setIntent(privacyIntent)
                .setIcon(R.drawable.about_icon_link);

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setDescription(getString(R.string.aboutLegal))
                .addItem(versionElement)
                .addGroup("Connect with us")
                .addEmail(getString(R.string.aboutEmail))
                .addPlayStore(getString(R.string.packageName))
                .addTwitter(getString(R.string.twitter))
                .addItem(gitHubElement)
                .addItem(offElement)
                .addItem(privacyElement)
                .create();

        setContentView(aboutPage);
    }
}
