package com.android.isem.goodforecast;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.isem.goodforecast.sync.GoodForecastSyncAdapter;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private String mLocationStr;

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getSupportActionBar().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_allweather_action_bar));

        mLocationStr = Utility.getPreferredLocation(this);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container,
                                new DetailFragment(),
                                DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;

            // We set the action bar elevation to be zero in one-pane mode.
            // This will get rid of an unnecessary shadow below
            // the action bar for smaller screen devices like phones.
            // Then the action bar and Today item will appear to be on the same plane
            // (as opposed to two different planes, where one casts a shadow on the other)
            getSupportActionBar().setElevation(0f);
        }

        ForecastFragment forecastFragment =  ((ForecastFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_forecast));
        forecastFragment.setUseTodayLayout(!mTwoPane);

        // MainActivity is created and the GoodForecastSyncAdapter is initializing
        GoodForecastSyncAdapter.initializeSyncAdapter(this);

        Log.v(LOG_TAG, "onCreate()");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check whether the location has changed by comparing whatever is stored in the settings
        // (Utility.getPreferredLocation(this)) with mLocationStr
        String location = Utility.getPreferredLocation(this);
        if (location != null && !location.equals(mLocationStr)) {
            // update the location in our second pane using the fragment manager
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_forecast);
            if (null != ff) {
                ff.onLocationChanged(); //update the weather forecast
            }
            DetailFragment df = (DetailFragment)getSupportFragmentManager()
                    .findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != df) {
                df.onLocationChanged(location);
            }
            mLocationStr = location;
        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            // One-pane mode
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, com.android.isem.goodforecast.SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
