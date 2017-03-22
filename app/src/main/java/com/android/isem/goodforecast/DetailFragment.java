package com.android.isem.goodforecast;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.isem.goodforecast.data.WeatherContract;

import java.util.Locale;

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    static final String DETAIL_URI = "URI";

    private ShareActionProvider shareActionProvider;

    private String forecastStr;

    private Uri mUri;

    private static final int DETAIL_LOADER_ID = 0;

    private static final String[] DETAIL_COLUMNS_PROJECTION = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    // Corresponding constants
    // Must change if the Projection changes
    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_WEATHER_HUMIDITY = 5;
    public static final int COL_WEATHER_PRESSURE = 6;
    public static final int COL_WEATHER_WIND_SPEED = 7;
    public static final int COL_WEATHER_DEGREES = 8;
    public static final int COL_WEATHER_CONDITION_ID = 9;

    private ImageView mIconView;
    private TextView mFriendlyDateView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mPressureView;
    private TextView mWindView;

    private CompassView mCmpassView;
    private TextView mWindDegreesView;

    public DetailFragment() {
        //Report that this fragment would like to participate in populating
        //the options menu by receiving a call to onCreateOptionsMenu() and related methods.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Read the arguments that the fragment was initialized with
        Bundle arguments = getArguments();

        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mFriendlyDateView = (TextView) rootView.findViewById(R.id.detail_day_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);

        mCmpassView = (CompassView) rootView.findViewById(R.id.compassView);
        mWindDegreesView = (TextView) rootView.findViewById(R.id.detail_wind_degrees_textview);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail_fragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (forecastStr != null) {
            shareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, forecastStr + getString(R.string.share_hashtag));

        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    public void onLocationChanged(String newLocationStr) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            // First, we try to figure out what is the date for the data that we're currently showing
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            // Then we create a new Uri with the new location and the date we're already displaying
            Uri updatedUri = WeatherContract.WeatherEntry
                    .buildWeatherLocationWithDate(newLocationStr, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "onCreateLoader()");

        if (null != mUri) {
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS_PROJECTION,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        Log.v(LOG_TAG, "onLoadFinished()");

        if (data != null && data.moveToFirst()) {

            // Read weather condition ID from cursor
            int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);
            // Use weather art image
            mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

            // Read date from cursor and update views for day of week and date
            long date = data.getLong(COL_WEATHER_DATE);
            String friendlyDateText = Utility.getDayName(getActivity(), date);
            String dateText = Utility.getFormattedMonthDay(getActivity(), date);
            mFriendlyDateView.setText(friendlyDateText);
            mDateView.setText(dateText);

            // Read description from cursor and update view
            String description = data.getString(COL_WEATHER_DESC);

            String language = Locale.getDefault().getLanguage();
            // Set weather description in line with current locale
            if (language.equals("uk") || language.equals("ru")) {
                mDescriptionView.setText(Utility.getLocaleForecastDescription(
                        getActivity(),
                        weatherId
                ));
            } else {
                mDescriptionView.setText(description);
            }

            // Accessibility: add a content description to the icon field
            mIconView.setContentDescription(description);

            // Read high temperature from cursor and update view
            double high = data.getDouble(COL_WEATHER_MAX_TEMP);
            String highString = Utility.formatTemperature(getActivity(), high);
            mHighTempView.setText(highString);

            // Read low temperature from cursor and update view
            double low = data.getDouble(COL_WEATHER_MIN_TEMP);
            String lowString = Utility.formatTemperature(getActivity(), low);
            mLowTempView.setText(lowString);

            // Read humidity from cursor and update view
            float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
            mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));

            // Read pressure from cursor and update view
            float pressure = data.getFloat(COL_WEATHER_PRESSURE);
            mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));

            // Read wind speed and direction from cursor and update view
            float windSpeedStr = data.getFloat(COL_WEATHER_WIND_SPEED);
            float windDirStr = data.getFloat(COL_WEATHER_DEGREES);
            String windDescription = Utility.getFormattedWind(getActivity(), windSpeedStr, windDirStr);
            mWindView.setText(windDescription);

            // Set wind direction in CompassView
            mCmpassView.update(windDirStr);
            mWindDegreesView.setText(getActivity().getString(
                    R.string.format_wind_direction_degrees, windDirStr));

            // Accessibility
            mCmpassView.setContentDescription(windDescription);

            // Forecast string for the share intent
            // Set weather description in line with current locale
            if (language.equals("uk") || language.equals("ru")) {
                forecastStr = String.format(
                        "%s : %s  %s/%s",
                        dateText,
                        Utility.getLocaleForecastDescription(
                                getActivity(),
                                weatherId),
                        highString,
                        lowString
                );
            } else {
                forecastStr = String.format(
                        "%s : %s  %s/%s",
                        dateText,
                        description,
                        highString,
                        lowString
                );
            }
            // If onCreateOptionsMenu has already happened, should to update the share intent now.
            if (shareActionProvider != null) {
                shareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
