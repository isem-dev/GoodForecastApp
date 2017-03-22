package com.android.isem.goodforecast;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.Time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utility {
    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    public static String formatTemperature(Context context, double temperature) {
        // Data stored in Celsius by default.  If user prefers to see in Fahrenheit, convert
        // the values here.
        if (!isMetric(context)) {
            temperature = (temperature * 1.8) + 32;
        }
        // Substituting the format arguments as defined in format(String, Object...).
        // For presentation, assume the user doesn't care about tenths of a degree.
        return String.format(
                context.getString(R.string.format_temperature), //Resource id for the format string
                temperature //The format arguments that will be used for substitution
        );
    }

    static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return DateFormat.getDateInstance().format(date);
    }

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateInMilliseconds The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, long dateInMilliseconds) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        int julianDay = Time.getJulianDay(dateInMilliseconds, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (julianDay == currentJulianDay) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateInMilliseconds)));
        } else if ( julianDay < currentJulianDay + 7 ) {
            // If the input date is less than a week in the future, just return the day name.
            return getDayName(context, dateInMilliseconds);
        } else {
            // Otherwise, use the form "Mon 3 Jun" OR "Mon Jun 3"
            String language = Locale.getDefault().getLanguage();
            SimpleDateFormat shortenedDateFormat;
            if (language.equals("uk") || language.equals("ru")) {
                shortenedDateFormat = new SimpleDateFormat("EEE dd MMM");
            } else {
                shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            }
            return shortenedDateFormat.format(dateInMilliseconds);
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateInMilliseconds The date in milliseconds
     * @return
     */
    public static String getDayName(Context context, long dateInMilliseconds) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMilliseconds, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return context.getString(R.string.tomorrow);
        } else {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMilliseconds);
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMilliseconds The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMilliseconds ) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);

        String language = Locale.getDefault().getLanguage();
        SimpleDateFormat monthDayFormat;
        if (language.equals("uk") || language.equals("ru")) {
            monthDayFormat = new SimpleDateFormat("dd MMMM");
        } else {
            monthDayFormat = new SimpleDateFormat("MMMM dd");
        }

        String monthDayString = monthDayFormat.format(dateInMilliseconds);
        return monthDayString;
    }

    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat;
        if (Utility.isMetric(context)) {
            windFormat = R.string.format_wind_kmh;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = context.getString(R.string.wind_direction_N);
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = context.getString(R.string.wind_direction_NE);
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = context.getString(R.string.wind_direction_E);
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = context.getString(R.string.wind_direction_SE);
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = context.getString(R.string.wind_direction_S);
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = context.getString(R.string.wind_direction_SW);
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = context.getString(R.string.wind_direction_W);
        } else if (degrees >= 292.5 || degrees < 22.5) {
            direction = context.getString(R.string.wind_direction_NW);
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    // Method provide weather description's string res for suitable locale
    public static String getLocaleForecastDescription(Context context, int weatherId) {
        if (weatherId >= 200 && weatherId <= 232) {
            return context.getString(R.string.storm);
        } else if (weatherId >= 300 && weatherId <= 321) {
            return context.getString(R.string.light_rain);
        } else if (weatherId >= 500 && weatherId <= 504) {
            return context.getString(R.string.rain);
        } else if (weatherId == 511) {
            return context.getString(R.string.snow);
        } else if (weatherId >= 520 && weatherId <= 531) {
            return context.getString(R.string.rain);
        } else if (weatherId >= 600 && weatherId <= 622) {
            return context.getString(R.string.snow);
        } else if (weatherId >= 701 && weatherId <= 761) {
            return context.getString(R.string.fog);
        } else if (weatherId == 761 || weatherId == 781) {
            return context.getString(R.string.storm);
        } else if (weatherId == 800) {
            return context.getString(R.string.clear);
        } else if (weatherId == 801) {
            return context.getString(R.string.light_clouds);
        } else if (weatherId >= 802 && weatherId <= 804) {
            return context.getString(R.string.clouds);
        }
        return null;
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }

    /**
     * Returns true if the network is available or about to become available.
     * @param c Context used to get the ConnectivityManager
     * @return
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}
