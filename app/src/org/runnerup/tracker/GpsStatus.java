/*
 * Copyright (C) 2012 jonas.oreland@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.runnerup.tracker;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;

import org.runnerup.util.TickListener;
/**
 * 
 * This is a helper class that is used to determine when the GPS status is good
 * enough (isFixed())
 * 
 */

@TargetApi(Build.VERSION_CODES.FROYO)
public class GpsStatus implements LocationListener,
        android.location.GpsStatus.Listener {

    static final int HIST_LEN = 3;

    boolean mIsFixed = false;
    Context context = null;
    Location mHistory[] = null;
    LocationManager locationManager = null;
    TickListener listener = null;

    /**
     * If we get a location with accurancy <= mFixAccurancy mFixed => true
     */
    final float mFixAccurancy = 10;

    /**
     * If we get fixed satellites >= mFixSatellites mFixed => true
     */
    final int mFixSatellites = 2;

    /**
     * If we get location updates with time difference <= mFixTime mFixed =>
     * true
     */
    final int mFixTime = 3;

    int mKnownSatellites = 0;
    int mUsedInLastFixSatellites = 0;

    public GpsStatus(Context ctx) {
        this.context = ctx;
        mHistory = new Location[HIST_LEN];
    }

    public void start(TickListener listener) {
        clear(true);
        this.listener = listener;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (Exception ex) {
            lm = null;
        }

        if (lm != null) {
            locationManager = lm;
            locationManager.addGpsStatusListener(this);
        }
    }

    public void stop(TickListener listener) {
        this.listener = null;
        if (locationManager != null) {
            locationManager.removeGpsStatusListener(this);
            locationManager.removeUpdates(this);
            locationManager = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        for (int i = 1; i < HIST_LEN; i++) {
            mHistory[i] = mHistory[i - 1];
        }
        mHistory[0] = location;
        if (location.hasAccuracy() && location.getAccuracy() < mFixAccurancy) {
            mIsFixed = true;
        } else if (mHistory[1] != null
                && (location.getTime() - mHistory[1].getTime()) <= (1000 * mFixTime)) {
            mIsFixed = true;
        } else if (mKnownSatellites >= mFixSatellites) {
            mIsFixed = true;
        }
        if (listener != null)
            listener.onTick();
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equalsIgnoreCase("gps")) {
            clear(true);
            if (listener != null)
                listener.onTick();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equalsIgnoreCase("gps")) {
            clear(false);
            if (listener != null)
                listener.onTick();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (provider.equalsIgnoreCase("gps")) {
            if (status == LocationProvider.OUT_OF_SERVICE
                    || status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                clear(true);
            }
            if (listener != null)
                listener.onTick();
        }
    }

    @Override
    public void onGpsStatusChanged(int event) {
        if (locationManager == null)
            return;

        android.location.GpsStatus gpsStatus = locationManager
                .getGpsStatus(null);

        if (gpsStatus == null)
            return;

        int cnt0 = 0, cnt1 = 0;
        Iterable<GpsSatellite> list = gpsStatus.getSatellites();
        for (GpsSatellite satellite : list) {
            cnt0++;
            if (satellite.usedInFix()) {
                cnt1++;
            }
        }
        mKnownSatellites = cnt0;
        mUsedInLastFixSatellites = cnt1;
        if (listener != null)
            listener.onTick();
    }

    private void clear(boolean resetIsFixed) {
        if (resetIsFixed) {
            mIsFixed = false;
        }
        mKnownSatellites = 0;
        mUsedInLastFixSatellites = 0;
        for (int i = 0; i < HIST_LEN; i++)
            mHistory[i] = null;
    }

    public boolean isLogging() {
        return locationManager != null;
    }

    public boolean isFixed() {
        return mIsFixed;
    }

    public int getSatellitesAvailable() {
        return mKnownSatellites;
    }

    public int getSatellitesFixed() {
        return mUsedInLastFixSatellites;
    }

    public boolean isEnabled() {
        LocationManager lm = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
