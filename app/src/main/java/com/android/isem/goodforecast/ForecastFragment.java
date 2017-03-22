package com.android.isem.goodforecast;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.isem.goodforecast.data.WeatherContract;
import com.android.isem.goodforecast.sync.GoodForecastSyncAdapter;

public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ForecastAdapter forecastAdapter;

    private ListView listView;

    private int currentPosition = ListView.INVALID_POSITION;

    private static final String SELECTED_KEY = "selected_position";

    /** Flag to determine if we want to use a separate view for "today". */
    private boolean useTodayLayoutFlag;

    private static final int FORECAST_LOADER_ID = 0;

    private static final String[] FORECAST_COLUMNS_PROJECTION = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    /**
     * These indices are tied to FORECAST_COLUMNS_PROJECTION.
     * If FORECAST_COLUMNS_PROJECTION changes, these
     * must change including the sequence of columns
     */
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_CONDITION_ID = 5;
    static final int COL_LOCATION_SETTING = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onItemSelected(Uri contentUri);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //In order for this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        if (null != forecastAdapter) {
            Cursor c = forecastAdapter.getCursor();
            if (null != c) {
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);

                /** Using the URI scheme for showing a location found on a map.
                 *  http://developer.android.com/guide/components/intents-common.html#Maps
                 */
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call "
                            + geoLocation.toString()
                            + ", no receiving apps installed!");
                }
            }
        }

    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        useTodayLayoutFlag = useTodayLayout;
        if (forecastAdapter != null) {
            forecastAdapter.setUseTodayLayout(useTodayLayoutFlag);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // The ForecastAdapter will take data from a source and
        // use it to populate the ListView it's attached to
        forecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        // Get a reference to the ListView, and attach this adapter to it
        listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        View emptyView = rootView.findViewById(R.id.listview_forecast_empty);
        listView.setEmptyView(emptyView);

        listView.setAdapter(forecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(
                                    WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE))
                            );
                }
                currentPosition = position;
            }
        });

        // Read currentPosition in listView.
        // If the app gets killed, then we can restore the position
        // from the savedInstanceState Bundle.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listView probably hasn't even been populated yet.
            // Actually perform the swapout in onLoadFinished method.
            currentPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        forecastAdapter.setUseTodayLayout(useTodayLayoutFlag);

        return rootView;
    }

    // Init Loader with LoaderManager
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    // Since we read the location when we create the loader, all we need to do is restart things
    void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
    }

    private void updateWeather() {
        GoodForecastSyncAdapter.syncImmediately(getActivity());
    }

    // Store currentPosition in the outState Bundle when list item selected.
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (currentPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, currentPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        String locationSetting = Utility.getPreferredLocation(getActivity());

        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS_PROJECTION,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        forecastAdapter.swapCursor(cursor);

        if (currentPosition != ListView.INVALID_POSITION) {
            listView.smoothScrollToPosition(currentPosition);
        }
        updateEmptyView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        forecastAdapter.swapCursor(null);
    }

    /**
     * Updates the empty list view with contextually relevant information that the user can
     * use to determine why they aren't seeing weather.
     */
    private void updateEmptyView() {
        if ( forecastAdapter.getCount() == 0 ) {
            TextView textView = (TextView) getView().findViewById(R.id.listview_forecast_empty);
            if ( null != textView ) {
                // If cursor is empty or an invalid location
                int message = R.string.empty_forecast_list;
                if (!Utility.isNetworkAvailable(getActivity()) ) {
                    message = R.string.empty_forecast_list_no_network;
                }
                textView.setText(message);
            }
        }
    }
}
