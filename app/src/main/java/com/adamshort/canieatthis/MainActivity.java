package com.adamshort.canieatthis;

import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends AppCompatActivity {

    private int position;

    private static final String CSV_URL = "http://world.openfoodfacts.org/data/en.openfoodfacts.org.products.csv";

    private List<Fragment> fragments;

    private ViewPager viewPager;
    private ResponseQuerier responseQuerier;
    private DataPasser dataPasser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_layout);

        fragments = new ArrayList<>();
        fragments.add(new ScanFragment());
        fragments.add(new PlacesFragment());

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        FragmentHandler fragmentHandler = new FragmentHandler(getFragmentManager(), fragments);
        viewPager.setAdapter(fragmentHandler);

        position = 0;

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setPosition(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        if (tabLayout != null) {
            tabLayout.setupWithViewPager(viewPager);
        }

        responseQuerier = ResponseQuerier.getInstance(this);
        dataPasser = DataPasser.getInstance();

        final RelativeLayout layout = new RelativeLayout(this);
        layout.setVisibility(View.VISIBLE);
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
        progressBar.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar, params);

        final LinearLayout parent = (LinearLayout) findViewById(R.id.tabLayoutLinearLayout);

        if (parent != null) {
            parent.addView(layout);

            FileDownloader fileDownloader = new FileDownloader(this, getBaseContext(), progressBar, new FileDownloader.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    File file = new File(String.format("%s/products.csv", getExternalStorageDirectory().getPath()));
                    if (file.exists()) {
                        Log.d("DEBUG", "CSV file downloaded successfully");
                    }
                    parent.removeView(layout);
                }
            });
            fileDownloader.execute(CSV_URL);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Menu actionMenu = menu;
        getMenuInflater().inflate(R.menu.main, actionMenu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) actionMenu.findItem(R.id.action_search).getActionView();
        if (null != searchView) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered
                return true;
            }

            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    // query is the value entered into the search bar
                    boolean dairy = responseQuerier.IsDairyFree(query);
                    boolean vegetarian = responseQuerier.IsVegetarian(query);
                    boolean vegan = responseQuerier.IsVegan(query);
                    boolean gluten = responseQuerier.IsGlutenFree(query);

                    dataPasser.setQuery(query);

                    dataPasser.setDairy(dairy);
                    dataPasser.setVegetarian(vegetarian);
                    dataPasser.setVegan(vegan);
                    dataPasser.setGluten(gluten);

                    dataPasser.setSwitchesVisible(true);
                    dataPasser.setItemVisible(true);
                    dataPasser.setIntroVisible(false);
                    dataPasser.setResponseVisible(false);

                    dataPasser.setFromSearch(true);

                    actionMenu.findItem(R.id.action_search).collapseActionView();
                    if (getPosition() != 0) {
                        viewPager.setCurrentItem(0);
                    } else {
                        ScanFragment scanFragment = (ScanFragment) fragments.get(0);
                        scanFragment.SetItemsFromDataPasser();
                    }
                }
                return true;
            }
        };

        if (searchView != null) {
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean queryTextFocused) {
                    if (!queryTextFocused) {
                        actionMenu.findItem(R.id.action_search).collapseActionView();
                        searchView.setQuery("", false);
                    }
                }
            });

            searchView.setOnQueryTextListener(queryTextListener);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

}
