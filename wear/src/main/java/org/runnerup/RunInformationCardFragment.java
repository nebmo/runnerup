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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.runnerup.common.WearConstants;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RunInformationCardFragment extends Fragment {
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;
    private RunInformationProvider mStateProvider;
    private TextView mTextField1;
    private TextView mTextField2;
    private TextView mTextField3;
    private TextView mTextField4;
    private TextView mTextFieldHeader1;
    private String mHeaderText1 = "";
    private ImageView mIconHeartRate;

    public static RunInformationCardFragment create(String title) {
        RunInformationCardFragment fragment = new RunInformationCardFragment();
        Bundle b = new Bundle();
        b.putString("title", title);
        fragment.setArguments(b);
        return fragment;
    }

    public RunInformationCardFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            mHeaderText1 = args.getString("title");
        }
        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats, container, false);
        mTextField1 = (TextView) view.findViewById(R.id.textView1);
        mTextField2 = (TextView) view.findViewById(R.id.textView2);
        mTextField3 = (TextView) view.findViewById(R.id.textView3);
        mTextField4 = (TextView) view.findViewById(R.id.textView4);
        mIconHeartRate = (ImageView) view.findViewById(R.id.imageHeart);
        mTextFieldHeader1 = (TextView) view.findViewById(R.id.stats_info_type);
        mTextFieldHeader1.setText(mHeaderText1);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mStateProvider != null)
                            setGuiDetails(mStateProvider.getRunInformation());
                    }
                }, 1, 1, TimeUnit.SECONDS);
    }

    private void setGuiDetails(final Bundle bundle) {
        if (bundle == null) return;
        if (bundle.getInt("activityStatus") != WearConstants.TYPE_GPS) return;

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mHeaderText1.equals(getActivity().getString(R.string.stats_title_total))) {
                    mTextField1.setText(bundle.getString("Activity Time"));
                    mTextField2.setText(bundle.getString("Activity Distance"));
                    mTextField3.setText(bundle.getString("Activity Pace"));
                    mTextField4.setText(bundle.getString("Current HR"));
                } else if (mHeaderText1.equals(getActivity().getString(R.string.stats_title_lap))) {
                    mTextField1.setText(bundle.getString("LAP Time"));
                    mTextField2.setText(bundle.getString("LAP Distance"));
                    mTextField3.setText(bundle.getString("LAP Pace"));
                    mTextField4.setText(bundle.getString("LAP HR"));
                } else if (bundle.getBoolean("IntervalActive")) {
                    mTextField1.setText(bundle.getString("Step Time"));
                    mTextField2.setText(bundle.getString("Step Distance"));
                    mTextField3.setText(bundle.getString("Step Pace"));
                    mTextField4.setText(bundle.getString("Step HR"));
                    mTextFieldHeader1.setText(bundle.getString("Step Info"));
                    mTextFieldHeader1.setTextAppearance(getActivity(), android.R.style.TextAppearance_DeviceDefault_Small);
                }
                if (mTextField4.getText().equals("") || mTextField4.getText().equals("")) {
                    mIconHeartRate.setVisibility(View.GONE);
                }
            }

        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mStateProvider = (RunInformationProvider) activity;
    }
}
