/*
 * Copyright (C) 2014 weides@gmail.com
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
package org.runnerup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class MainActivity extends Activity implements RunInformationProvider, View.OnTouchListener {

    private StateService mCurrentStateProviderService = null;
    private DismissOverlayView mDismissOverlay;
    private GestureDetector mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(new RunnerUpGridPagerAdapter(this, getFragmentManager()));
        pager.setOnTouchListener(this);
        // Obtain the DismissOverlayView element
        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText(R.string.exit);
        mDismissOverlay.showIntroIfNecessary();

        // Configure a gesture detector
        mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent ev) {
                mDismissOverlay.show();
            }
        });
    }

    // Capture long presses
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getApplicationContext().unbindService(mStateServiceConnection);
        mCurrentStateProviderService = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getApplicationContext().bindService(new Intent(this, StateService.class),
                mStateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mStateServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mCurrentStateProviderService == null) {
                mCurrentStateProviderService = ((StateService.LocalBinder) service).getService();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCurrentStateProviderService = null;
        }
    };

    @Override
    public Bundle getRunInformation() {
        return mCurrentStateProviderService != null ? mCurrentStateProviderService.getCurrentInfo() : null;
    }

}
