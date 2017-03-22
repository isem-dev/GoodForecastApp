package com.android.isem.goodforecast.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class GoodForecastSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static GoodForecastSyncAdapter sGoodForecastSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("GoodForecastSyncService", "onCreate - GoodForecastSyncService");
        synchronized (sSyncAdapterLock) {
            if (sGoodForecastSyncAdapter == null) {
                sGoodForecastSyncAdapter = new GoodForecastSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sGoodForecastSyncAdapter.getSyncAdapterBinder();
    }
}