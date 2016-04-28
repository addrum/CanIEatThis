package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String CSV_URL = "http://world.openfoodfacts.org/data/en.openfoodfacts.org.products.csv";

    private int position;

    private List<Fragment> fragments;

    private ViewPager viewPager;
    private ResponseQuerier responseQuerier;
    private DataPasser dataPasser;

    private DownloadManager downloadManager;

    private BroadcastReceiver downloadCompleteReceiver;

    private FileDownloader fileDownloader;

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

        final Activity activity = this;

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        String status = prefs.getString("download_status", "null");
        if (!status.equals("downloading")) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Database Update Available");
            dialog.setMessage("A new database update is available for download. Download now?");
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    fileDownloader = new FileDownloader(activity, downloadManager, CSV_URL);
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        }

        createBroadcastCompleteReceiver();
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

    private void createBroadcastCompleteReceiver() {
        Log.d("DEBUG", "Registering download complete receiver");
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (fileDownloader.getDownloadReference() == reference) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(reference);
                    Cursor cursor = downloadManager.query(query);

                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);

                    int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                    String savedFilePath = cursor.getString(fileNameIndex);

                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            Toast.makeText(MainActivity.this, "Successfully downloaded database update", Toast.LENGTH_LONG).show();
                            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("download_status", "downloaded");
                            editor.apply();
                            break;
                        case DownloadManager.STATUS_FAILED:
                            Toast.makeText(MainActivity.this, "FAILED: " + reason, Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            Toast.makeText(MainActivity.this, "PAUSED: " + reason, Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PENDING:
                            Toast.makeText(MainActivity.this, "PENDING!", Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            Toast.makeText(MainActivity.this, "RUNNING!", Toast.LENGTH_LONG).show();
                            break;
                    }
                    cursor.close();
                }
            }
        };
        registerReceiver(downloadCompleteReceiver, intentFilter);
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

    @Override
    protected void onResume() {
        super.onResume();

        createBroadcastCompleteReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadCompleteReceiver);
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

}
