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
import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.CircledImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.runnerup.common.Constants;
import org.runnerup.common.WearConstants;

public class PauseResumeCardFragment extends Fragment {

    private static final String TAG = "PauseResumeCardFragment";
    private CircledImageView mButtonPauseResume;
    private CircledImageView mButtonNewLap;
    private TextView mLabel;
    private GoogleApiClient mGoogleAppiClient;
    private RunInformationProvider mStateProvider;

    public static PauseResumeCardFragment create(int labelResId) {
        PauseResumeCardFragment fragment = new PauseResumeCardFragment();
        Bundle args = new Bundle();
        args.putInt("LABEL", labelResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleAppiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the data layer API
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pause_resume, container, false);

        super.onViewCreated(view, savedInstanceState);
        mLabel = (TextView) view.findViewById(R.id.label);
        mButtonPauseResume = (CircledImageView) view.findViewById(R.id.icon);
        mButtonPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Wearable.MessageApi.sendMessage(mGoogleAppiClient, Constants.Intents.START_STOP, Constants.Intents.START_STOP, null);
                if (mLabel.getText().toString().equals(getString(R.string.start))) {
                    mLabel.setText(R.string.pause);
                    mButtonPauseResume.setImageResource(R.drawable.ic_av_pause);
                } else {
                    mLabel.setText(R.string.start);
                    mButtonPauseResume.setImageResource(R.drawable.ic_av_play_arrow);
                }
            }
        });
        mButtonNewLap = (CircledImageView) view.findViewById(R.id.icon2);
        mButtonNewLap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Wearable.MessageApi.sendMessage(mGoogleAppiClient, Constants.Intents.NEW_LAP, Constants.Intents.NEW_LAP, null);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleAppiClient.connect();
        if (mStateProvider.getRunInformation() == null) return;
        int state = mStateProvider.getRunInformation().getInt("activityStatus");

        switch (state) {
            case WearConstants.TYPE_START:
            case WearConstants.TYPE_RESUME:
            case WearConstants.TYPE_GPS:
                mLabel.setText(R.string.pause);
                break;
            case WearConstants.TYPE_PAUSE:
            case WearConstants.TYPE_END:
                mLabel.setText(R.string.start);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleAppiClient.disconnect();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mStateProvider = (RunInformationProvider) activity;
    }
}
