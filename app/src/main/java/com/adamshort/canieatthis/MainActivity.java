package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private int position;
    private ViewPager viewPager;
    private ResponseQuerier responseQuerier;
    private List<Fragment> fragments;

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
        tabLayout.setupWithViewPager(viewPager);

        responseQuerier = ResponseQuerier.getInstance(this);
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

                    DataPasser.getInstance().setQuery(query);

                    DataPasser.getInstance().setDairy(dairy);
                    DataPasser.getInstance().setVegetarian(vegetarian);
                    DataPasser.getInstance().setVegan(vegan);
                    DataPasser.getInstance().setGluten(gluten);

                    DataPasser.getInstance().setSwitchesVisible(true);
                    DataPasser.getInstance().setItemVisible(true);
                    DataPasser.getInstance().setIntroVisible(false);
                    DataPasser.getInstance().setResponseVisible(false);

                    DataPasser.getInstance().setFromSearch(true);

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

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

}
