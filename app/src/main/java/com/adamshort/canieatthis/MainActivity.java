package com.adamshort.canieatthis;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_layout);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new FragmentPageAdapter(getSupportFragmentManager(),
                MainActivity.this));

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Menu actionMenu = menu;
        getMenuInflater().inflate(R.menu.main, actionMenu);

        // Define the listener
        MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when action item collapses
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;  // Return true to expand action view
            }
        };

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
                    /*boolean dairy = IsDairyFree(query);
                    boolean vegetarian = IsVegetarian(query);
                    boolean vegan = IsVegan(query);
                    boolean gluten = IsGlutenFree(query);
                    SetAllergenSwitches(dairy, vegetarian, vegan, gluten);
                    actionMenu.findItem(R.id.action_search).collapseActionView();
                    itemTextView.setText(String.format(getString(R.string.ingredient), query));
                    introTextView.setVisibility(View.INVISIBLE);
                    itemTextView.setVisibility(View.VISIBLE);
                    SetSwitchesVisibility(View.VISIBLE);
                    SetResponseItemsVisibility(View.INVISIBLE);*/
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


        // Assign the listener to that action item
        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.action_search), expandListener);

        return super.onCreateOptionsMenu(menu);
    }

}
