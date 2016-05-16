package com.adamshort.canieatthis;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.firebase.client.Firebase;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.adamshort.canieatthis.DataQuerier.*;

public class MainActivity extends AppCompatActivity {

    private static final String CSV_URL = "http://world.openfoodfacts.org/data/en.openfoodfacts.org.products.csv";

    private int position;

    private List<Fragment> fragments;
    private ViewPager viewPager;
    private DataPasser dataPasser;
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadCompleteReceiver;
    private FileDownloader fileDownloader;
    private PlacesFragment placesFragment;
    private LinearLayout tabLayoutLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Firebase.getDefaultConfig().isPersistenceEnabled()) {
            Firebase.getDefaultConfig().setPersistenceEnabled(true);
        }
        Firebase.setAndroidContext(getBaseContext());

        fragments = new ArrayList<>();
        fragments.add(new ScanFragment());
        placesFragment = new PlacesFragment();
        fragments.add(placesFragment);

        tabLayoutLinearLayout = (LinearLayout) findViewById(R.id.tabLayoutLinearLayout);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        FragmentHandler fragmentHandler = new FragmentHandler(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(fragmentHandler);

        Intent intent = getIntent();
        position = intent.getIntExtra("position", 0);
        viewPager.setCurrentItem(position);

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

        dataPasser = DataPasser.getInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Menu actionMenu = menu;
        getMenuInflater().inflate(R.menu.menu, actionMenu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) actionMenu.findItem(R.id.action_search).getActionView();
        if (null != searchView) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setQueryHint(getString(R.string.searchViewQueryHint));
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered
                return true;
            }

            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    // query is the value entered into the search bar
                    boolean dairy = isLactoseFree(query);
                    boolean vegan = isVegan(query);
                    boolean vegetarian = false;
                    // if something is vegan it is 100% vegetarian
                    if (!vegan) {
                        vegetarian = isVegetarian(query);
                    }
                    boolean gluten = isGlutenFree(query);

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
                        scanFragment.setItemsFromDataPasser();
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
        Log.d("createBroadcast", "Registering download complete receiver");
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
                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);

                    SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            Snackbar.make(tabLayoutLinearLayout, "Successfully downloaded database update", Snackbar.LENGTH_LONG).show();
//                            Toast.makeText(MainActivity.this, "Successfully downloaded database update", Toast.LENGTH_LONG).show();
                            editor.putString("download_status", "downloaded");
                            editor.apply();
                            String internalDir = getExternalFilesDir(null).getPath();
                            File from = new File(internalDir, "products.csv.tmp");
                            File to = new File(internalDir, "products.csv");
                            boolean success = from.renameTo(to);
                            Log.d("DEBUG", "Renamed: " + success);

                            break;
                        case DownloadManager.STATUS_FAILED:
                            Log.d("onReceive", "Download failed: " + reason);
                            Snackbar.make(tabLayoutLinearLayout, "Database update failed", Snackbar.LENGTH_LONG).show();
//                            Toast.makeText(MainActivity.this, "Database update download failed: " + reason, Toast.LENGTH_LONG).show();
                            editor.putString("download_status", "failed");
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            editor.putString("download_status", "paused");
                            break;
                        case DownloadManager.STATUS_PENDING:
                            editor.putString("download_status", "pending");
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            editor.putString("download_status", "running");
                            break;
                    }
                    cursor.close();
                }
            }
        };
        registerReceiver(downloadCompleteReceiver, intentFilter);
    }

    private void showDownloadPrompt() {
        if (hasInternetConnection()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            boolean downloadDatabasePref = preferences.getBoolean("download_switch_pref", false);
            Log.d("showDownloadPrompt", "Should download database: " + downloadDatabasePref);

            if (downloadDatabasePref) {
                SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                String status = prefs.getString("download_status", "null");
                Log.d("showDownloadPrompt", "Download Status: " + status);
                if (!status.equals("downloading")) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setTitle("Database Update Available");
                    dialog.setMessage("A new database update is available for download. Download now?");
                    dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            downloadDatabase();
                        }
                    });
                    dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    dialog.show();
                }
            }
        } else {
            Log.d("showDownloadPrompt", "Has no internet connection");
        }
    }

    private void downloadDatabase() {
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        fileDownloader = new FileDownloader(this, downloadManager, CSV_URL, "products.csv.tmp");

        // Update timestamp since we've downloaded a new one
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("timestamp", System.currentTimeMillis());
        editor.apply();
    }

    public boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            return activeNetwork.isConnected();
        }
        return false;
    }

    public boolean timeForUpdatePrompt(Timestamp current) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        long lastPref = prefs.getLong("timestamp", current.getTime());
        if (lastPref == 0) {
            // Pref is probably empty when app is first installed so set it to current time as default
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("timestamp", System.currentTimeMillis());
            editor.apply();
        }
        Timestamp last = new Timestamp(lastPref);

        Log.d("timeForUpdatePrompt", "Current: " + current.getTime() + " - Last: " + last.getTime());

        String frequency = prefs.getString("frequency_list_pref", "0");
        Log.d("timeForUpdatePrompt", "Frequency is " + frequency);

        long days = TimeUnit.MILLISECONDS.toDays(current.getTime() - last.getTime());
        Log.d("timeForUpdatePrompt", "Days is " + days);

        switch (frequency) {
            case "0":
                if (days > 1) {
                    return true;
                }
                break;
            case "1":
                if (days > 7) {
                    return true;
                }
                break;
            case "2":
                if (days > 28) {
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (placesFragment != null) {
            placesFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                this.startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_about:
                this.startActivity(new Intent(this, AboutActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        createBroadcastCompleteReceiver();
        long current = System.currentTimeMillis();
        Timestamp cur = new Timestamp(current);
        if (timeForUpdatePrompt(cur)) {
            Log.d("onResume", "Time for update prompt was true");
            showDownloadPrompt();
        }
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
