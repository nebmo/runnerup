/*
 * Copyright (C) 2012 - 2013 jonas.oreland@gmail.com
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
package org.runnerup.view;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import org.runnerup.R;
import org.runnerup.db.DBHelper;
import org.runnerup.gpstracker.GpsTracker;
import org.runnerup.hr.MockHRProvider;
import org.runnerup.util.Constants.DB;
import org.runnerup.util.Formatter;
import org.runnerup.util.SafeParse;
import org.runnerup.util.TickListener;
import org.runnerup.widget.StepButton;
import org.runnerup.widget.TitleSpinner;
import org.runnerup.widget.TitleSpinner.OnCloseDialogListener;
import org.runnerup.widget.TitleSpinner.OnSetValueListener;
import org.runnerup.widget.WidgetUtil;
import org.runnerup.workout.Dimension;
import org.runnerup.workout.HeadsetButtonReceiver;
import org.runnerup.workout.Workout;
import org.runnerup.workout.Workout.StepListEntry;
import org.runnerup.workout.WorkoutBuilder;
import org.runnerup.workout.WorkoutSerializer;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class StartFragment extends Fragment implements TickListener {
	final static String TAB_BASIC    = "basic";
	final static String TAB_INTERVAL = "interval";
	final static String TAB_ADVANCED = "advanced";
	final static String TAB_MANUAL   = "manual";
	
	boolean skipStopGps = false;
	GpsTracker mGpsTracker = null;
	org.runnerup.gpstracker.GpsStatus mGpsStatus = null;

	TabHost tabHost = null;
	Button startButton = null;
	TextView gpsInfoView1 = null;
	TextView gpsInfoView2 = null;
	View gpsInfoLayout = null;
	TextView hrInfo = null;
	
	ImageButton hrButton = null; 
	TextView hrValueText = null;
	FrameLayout hrLayout = null;
	
	TitleSpinner simpleType = null;
	TitleSpinner simpleTime = null;
	TitleSpinner simpleDistance = null;
	TitleSpinner simpleAudioSpinner = null;
	AudioSchemeListAdapter simpleAudioListAdapter = null;
	TitleSpinner simpleTargetType = null;
	TargetEntriesAdapter targetEntriesAdapter = null;
	TitleSpinner simpleTargetPaceValue = null;
	TitleSpinner simpleTargetHrz;
	HRZonesListAdapter hrZonesAdapter = null;

	TitleSpinner intervalType = null;
	TitleSpinner intervalTime = null;
	TitleSpinner intervalDistance = null;
	TitleSpinner intervalRestType = null;
	TitleSpinner intervalRestTime = null;
	TitleSpinner intervalRestDistance = null;
	TitleSpinner intervalAudioSpinner = null;
	AudioSchemeListAdapter intervalAudioListAdapter = null;

	TitleSpinner advancedWorkoutSpinner = null;
	WorkoutListAdapter advancedWorkoutListAdapter = null;
	TitleSpinner advancedAudioSpinner = null;
	AudioSchemeListAdapter advancedAudioListAdapter = null;
	Button       advancedDownloadWorkoutButton = null;
	Workout      advancedWorkout = null;
	ListView     advancedStepList = null;
	WorkoutStepsAdapter advancedWorkoutStepsAdapter = new WorkoutStepsAdapter();
		
	boolean manualSetValue = false;
	TitleSpinner manualDate = null;
	TitleSpinner manualTime = null;
	TitleSpinner manualDistance = null;
	TitleSpinner manualDuration = null;
	TitleSpinner manualPace = null;
	EditText     manualNotes = null;

	DBHelper mDBHelper = null;
	SQLiteDatabase mDB = null;
	
	Formatter formatter = null;
	BroadcastReceiver catchButtonEvent = null;
	boolean allowHardwareKey = false;
	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mDBHelper = new DBHelper(getActivity());
		mDB = mDBHelper.getWritableDatabase();
		formatter = new Formatter(getActivity());
		
		bindGpsTracker();
		mGpsStatus = new org.runnerup.gpstracker.GpsStatus(getActivity());

		if (getArguments() != null) {
			Bundle i = getArguments();
            if (TAB_ADVANCED.equals(i.getString("mode"))) {
                tabHost.setCurrentTab(2);
                i.putString("mode", null);
            }
		}
		
		catchButtonEvent = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				startButton.performClick();
			}
		};

//		if (getAllowStartStopFromHeadsetKey()) {
//			registerHeadsetListener();
//		}
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start, container, false);

        startButton = (Button) view.findViewById(R.id.startButton);
        startButton.setOnClickListener(startButtonClick);
        gpsInfoLayout = view.findViewById(R.id.GPSINFO);
        gpsInfoView1 = (TextView) view.findViewById(R.id.gpsInfo1);
        gpsInfoView2 = (TextView) view.findViewById(R.id.gpsInfo2);
        hrInfo = (TextView) view.findViewById(R.id.hrInfo);

        hrButton = (ImageButton) view.findViewById(R.id.hrButton);
        hrButton.setOnClickListener(hrButtonClick);
        hrValueText = (TextView) view.findViewById(R.id.hrValueText);
        hrLayout = (FrameLayout) view.findViewById(R.id.hrLayout);

        tabHost = (TabHost) view.findViewById(R.id.tabhostStart);
        tabHost.setup();
        TabSpec tabSpec = tabHost.newTabSpec(TAB_BASIC);
        tabSpec.setIndicator(WidgetUtil.createHoloTabIndicator(getActivity(), "Basic"));
        tabSpec.setContent(R.id.tabBasic);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec(TAB_INTERVAL);
        tabSpec.setIndicator(WidgetUtil.createHoloTabIndicator(getActivity(), "Interval"));
        tabSpec.setContent(R.id.tabInterval);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec(TAB_ADVANCED);
        tabSpec.setIndicator(WidgetUtil.createHoloTabIndicator(getActivity(), "Advanced"));
        tabSpec.setContent(R.id.tabAdvanced);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec(TAB_MANUAL);
        tabSpec.setIndicator(WidgetUtil.createHoloTabIndicator(getActivity(), "Manual"));
        tabSpec.setContent(R.id.tabManual);
        tabHost.addTab(tabSpec);

        tabHost.setOnTabChangedListener(onTabChangeListener);
        tabHost.getTabWidget().setBackgroundColor(Color.DKGRAY);

        CheckBox goal = (CheckBox) view.findViewById(R.id.tabBasicGoal);
        goal.setOnCheckedChangeListener(simpleGoalOnCheckClick);
        simpleType = (TitleSpinner) view.findViewById(R.id.basicType);
        simpleTime = (TitleSpinner) view.findViewById(R.id.basicTime);
        simpleDistance = (TitleSpinner) view.findViewById(R.id.basicDistance);
        simpleType.setOnSetValueListener(simpleTypeSetValue);
        simpleGoalOnCheckClick.onCheckedChanged(goal, goal.isChecked());
        simpleAudioListAdapter = new AudioSchemeListAdapter(mDB, inflater, false);
        simpleAudioListAdapter.reload();
        simpleAudioSpinner = (TitleSpinner) view.findViewById(R.id.basicAudioCueSpinner);
        simpleAudioSpinner.setAdapter(simpleAudioListAdapter);
        simpleTargetType = (TitleSpinner) view.findViewById(R.id.tabBasicTargetType);
        simpleTargetPaceValue = (TitleSpinner) view.findViewById(R.id.tabBasicTargetPaceMax);
        hrZonesAdapter = new HRZonesListAdapter(getActivity(), inflater);
        simpleTargetHrz = (TitleSpinner) view.findViewById(R.id.tabBasicTargetHrz);
        simpleTargetHrz.setAdapter(hrZonesAdapter);
        simpleTargetType.setOnCloseDialogListener(simpleTargetTypeClick);
        targetEntriesAdapter = new TargetEntriesAdapter(getActivity());

        intervalType = (TitleSpinner) view.findViewById(R.id.intervalType);
        intervalTime = (TitleSpinner) view.findViewById(R.id.intervalTime);
        intervalTime.setOnSetValueListener(onSetTimeValidator);
        intervalDistance = (TitleSpinner) view.findViewById(R.id.intervalDistance);
        intervalType.setOnSetValueListener(intervalTypeSetValue);

        intervalRestType = (TitleSpinner) view.findViewById(R.id.intervalRestType);
        intervalRestTime = (TitleSpinner) view.findViewById(R.id.intervalRestTime);
        intervalRestTime.setOnSetValueListener(onSetTimeValidator);
        intervalRestDistance = (TitleSpinner) view.findViewById(R.id.intervalRestDistance);
        intervalRestType.setOnSetValueListener(intervalRestTypeSetValue);
        intervalAudioListAdapter = new AudioSchemeListAdapter(mDB, inflater, false);
        intervalAudioListAdapter.reload();
        intervalAudioSpinner = (TitleSpinner) view.findViewById(R.id.intervalAudioCueSpinner);
        intervalAudioSpinner.setAdapter(intervalAudioListAdapter);

        advancedAudioListAdapter = new AudioSchemeListAdapter(mDB, inflater, false);
        advancedAudioListAdapter.reload();
        advancedAudioSpinner = (TitleSpinner) view.findViewById(R.id.advancedAudioCueSpinner);
        advancedAudioSpinner.setAdapter(advancedAudioListAdapter);
        advancedWorkoutSpinner = (TitleSpinner) view.findViewById(R.id.advancedWorkoutSpinner);
        advancedWorkoutListAdapter = new WorkoutListAdapter(inflater);
        advancedWorkoutListAdapter.reload();
        advancedWorkoutSpinner.setAdapter(advancedWorkoutListAdapter);
        advancedWorkoutSpinner.setOnSetValueListener(new OnSetValueListener() {
            @Override
            public String preSetValue(String newValue)
                    throws IllegalArgumentException {
                loadAdvanced(newValue);
                return newValue;
            }

            @Override
            public int preSetValue(int newValue)
                    throws IllegalArgumentException {
                loadAdvanced(null);
                return newValue;
            }});
        advancedStepList = (ListView) view.findViewById(R.id.advancedStepList);
        advancedStepList.setDividerHeight(0);
        advancedStepList.setAdapter(advancedWorkoutStepsAdapter);
        advancedDownloadWorkoutButton = (Button) view.findViewById(R.id.advancedDownloadButton);
        advancedDownloadWorkoutButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ManageWorkoutsActivity.class);
                StartFragment.this.startActivityForResult(intent, 113);
            }});

        manualDate = (TitleSpinner) view.findViewById(R.id.manualDate);
        manualDate.setOnSetValueListener(onSetValueManual);
        manualTime = (TitleSpinner) view.findViewById(R.id.manualTime);
        manualTime.setOnSetValueListener(onSetValueManual);
        manualDistance = (TitleSpinner) view.findViewById(R.id.manualDistance);
        manualDistance.setOnSetValueListener(onSetManualDistance);
        manualDuration = (TitleSpinner) view.findViewById(R.id.manualDuration);
        manualDuration.setOnSetValueListener(onSetManualDuration);
        manualPace = (TitleSpinner) view.findViewById(R.id.manualPace);
        manualPace.setVisibility(View.GONE);
        manualNotes = (EditText) view.findViewById(R.id.manualNotes);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateTargetView();
    }

    @Override
	public void onPause() {
		super.onPause();

		if (getAutoStartGps()) {
			/**
			 * If autoStartGps, then stop it during pause
			 */
			stopGps();
		}
	}

	
	@Override
	public void onResume() {
		super.onResume();
		simpleAudioListAdapter.reload();
		intervalAudioListAdapter.reload();
		advancedAudioListAdapter.reload();
		advancedWorkoutListAdapter.reload();
		hrZonesAdapter.reload();
		simpleTargetHrz.setAdapter(hrZonesAdapter);
		if (!hrZonesAdapter.hrZones.isConfigured()) {
			targetEntriesAdapter.addDisabled(2);
		} else if (targetEntriesAdapter.disabled != null){
			targetEntriesAdapter.disabled.clear();
		}
		simpleTargetType.setAdapter(targetEntriesAdapter);

		if (tabHost.getCurrentTabTag().contentEquals(TAB_ADVANCED)) {
			loadAdvanced(null);
		}

		if (mIsBound == false || mGpsTracker == null) {
			bindGpsTracker();
		} else {
			onGpsTrackerBound();
		}
		if (getAllowStartStopFromHeadsetKey()) {
			unregisterHeadsetListener();
			registerHeadsetListener();
		}
	}

	private void registerHeadsetListener() {
		ComponentName mMediaReceiverCompName = new ComponentName(
				getActivity().getPackageName(), HeadsetButtonReceiver.class.getName());
		AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
		mAudioManager
				.registerMediaButtonEventReceiver(mMediaReceiverCompName);
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.setPriority(2147483647);
		intentFilter.addAction("org.runnerup.START_STOP");
		getActivity().registerReceiver(catchButtonEvent, intentFilter);
	}

	private void unregisterHeadsetListener() {
		ComponentName mMediaReceiverCompName = new ComponentName(
				getActivity().getPackageName(), HeadsetButtonReceiver.class.getName());
		AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
		mAudioManager
				.unregisterMediaButtonEventReceiver(mMediaReceiverCompName);
		try {
			getActivity().unregisterReceiver(catchButtonEvent);
		} catch (IllegalArgumentException e) {
			if (e.getMessage().contains("Receiver not registered")) {
			} else {
				// unexpected, re-throw
				throw e;
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopGps();
		unbindGpsTracker();
		mGpsStatus = null;
		mGpsTracker = null;
		
		mDB.close();
		mDBHelper.close();
	}

	void onGpsTrackerBound() {
		if (getAutoStartGps()) {
			startGps();
		} else {
		}
		updateView();
	}

	boolean getAutoStartGps() {
		Context ctx = getActivity().getApplicationContext();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx); 
		return pref.getBoolean("pref_startgps", false);
	}
	
	boolean getAllowStartStopFromHeadsetKey() {
		Context ctx = getActivity().getApplicationContext();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx); 
		return pref.getBoolean("pref_keystartstop_active", true);
	}

	private void startGps() {
		System.err.println("StartActivity.startGps()");
		if (mGpsStatus != null && !mGpsStatus.isLogging())
			mGpsStatus.start(this);
		if (mGpsTracker != null && !mGpsTracker.isLogging())
			mGpsTracker.startLogging();
	}

	private void stopGps() {
		System.err.println("StartActivity.stopGps() skipStop: " + this.skipStopGps);
		if (skipStopGps == true)
			return;
		
		if (mGpsStatus != null)
			mGpsStatus.stop(this);

		if (mGpsTracker != null)
			mGpsTracker.stopLogging();
	}
	
	OnTabChangeListener onTabChangeListener = new OnTabChangeListener() {

		@Override
		public void onTabChanged(String tabId) {
			if (tabId.contentEquals(TAB_BASIC))
				startButton.setVisibility(View.VISIBLE);
			else if (tabId.contentEquals(TAB_INTERVAL))
				startButton.setVisibility(View.VISIBLE);
			else if (tabId.contentEquals(TAB_ADVANCED)) {
				startButton.setVisibility(View.VISIBLE);
				loadAdvanced(null);
			} else if (tabId.contentEquals(TAB_MANUAL)) {
				startButton.setText("Save activity");
			}
			updateView();
		}
	};

	OnClickListener startButtonClick = new OnClickListener() {
		public void onClick(View v) {
			
			
			if (tabHost.getCurrentTabTag().contentEquals(TAB_MANUAL)) {
				manualSaveButtonClick.onClick(v);
				return;
			} else if (mGpsStatus.isEnabled() == false) {
				startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			} else if (mGpsTracker.isLogging() == false) {
				startGps();
			} else if (mGpsStatus.isFixed()) {
				Context ctx = getActivity().getApplicationContext();
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
				SharedPreferences audioPref = null;
				Workout w = null;
				if (tabHost.getCurrentTabTag().contentEquals(TAB_BASIC)) {
					audioPref = WorkoutBuilder.getAudioCuePreferences(ctx, pref, "basicAudio");
					Dimension target = null;
					switch(simpleTargetType.getValueInt()) {
					case 0: // none
						break;
					case 1:
						target = Dimension.PACE;
						break;
					case 2:
						target = Dimension.HRZ;
						break;
					}
					w = WorkoutBuilder.createDefaultWorkout(getResources(), pref, target);
				}
				else if (tabHost.getCurrentTabTag().contentEquals(TAB_INTERVAL)) {
					audioPref = WorkoutBuilder.getAudioCuePreferences(ctx, pref, "intervalAudio");
					w = WorkoutBuilder.createDefaultIntervalWorkout(getResources(),pref);
				}
				else if (tabHost.getCurrentTabTag().contentEquals(TAB_ADVANCED)) {
					audioPref = WorkoutBuilder.getAudioCuePreferences(ctx, pref, "advancedAudio");
					w = advancedWorkout;
				}
				skipStopGps = true;
				WorkoutBuilder.prepareWorkout(getResources(), pref, w, TAB_BASIC.contentEquals(tabHost.getCurrentTabTag()));
				WorkoutBuilder.addAudioCuesToWorkout(getResources(), w, audioPref);
				mGpsStatus.stop(StartFragment.this);
				mGpsTracker.setWorkout(w);
				
				Intent intent = new Intent(getActivity(),
						RunActivity.class);
				StartFragment.this.startActivityForResult(intent, 112);
				if (getAllowStartStopFromHeadsetKey()){
					unregisterHeadsetListener();
				}
				return;
			}
			updateView();
		}
	};

	OnClickListener hrButtonClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {

		}
	};
	
	private void updateView() {
		{
			int cnt0 = mGpsStatus.getSatellitesFixed();
			int cnt1 = mGpsStatus.getSatellitesAvailable();
			gpsInfoView1.setText("" + cnt0 + "/" + cnt1);
		}

		String gpsInfo2 = "";
		if (mGpsTracker != null) {
			Location l = mGpsTracker.getLastKnownLocation();

			if (l != null && l.getAccuracy() > 0) {
				gpsInfo2 = ", " + l.getAccuracy() + "m";
			}
		}
		gpsInfoView2.setText(gpsInfo2);
		
		if (tabHost.getCurrentTabTag().contentEquals(TAB_MANUAL)) {
			gpsInfoLayout.setVisibility(View.GONE);
			startButton.setEnabled(manualSetValue);
			startButton.setText("Save activity");
			return;
		} else if (mGpsStatus.isEnabled() == false) {
			startButton.setEnabled(true);
			startButton.setText("Enable GPS");
		} else if (mGpsStatus.isLogging() == false) {
			startButton.setEnabled(true);
			startButton.setText("Start GPS");
		} else if (mGpsStatus.isFixed() == false) {
			startButton.setEnabled(false);
			startButton.setText("Waiting for GPS");
		} else {
			startButton.setText("Start activity");
			if (!tabHost.getCurrentTabTag().contentEquals(TAB_ADVANCED) || advancedWorkout != null) {
				startButton.setEnabled(true);
			} else {
				startButton.setEnabled(false);
			}
		}
		gpsInfoLayout.setVisibility(View.VISIBLE);
		
		{
			Resources res = getResources();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			final String btDeviceName = prefs.getString(res.getString(R.string.pref_bt_name), null);
			if (btDeviceName != null) {
				hrInfo.setText(btDeviceName);
			} else {
				hrInfo.setText("");
				if (MockHRProvider.NAME.contentEquals(prefs.getString(res.getString(R.string.pref_bt_provider), ""))) {
					final String btAddress = "mock: " + prefs.getString(res.getString(R.string.pref_bt_address), "???");
					hrInfo.setText(btAddress);
				}
			}
		}
		
		if (mGpsTracker != null && mGpsTracker.isHRConfigured()) {
			hrLayout.setVisibility(View.VISIBLE);
			Integer hrVal = null;
			if (mGpsTracker.isHRConnected()) {
				hrVal = mGpsTracker.getCurrentHRValue();
			}
			if (hrVal != null) {
				hrButton.setEnabled(false);
				hrValueText.setText(Integer.toString(hrVal));
			} else {
				hrButton.setEnabled(true);
				hrValueText.setText("?");
			}
		}
		else {
			hrLayout.setVisibility(View.GONE);
		}
	}
	
	private boolean mIsBound = false;
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mGpsTracker = ((GpsTracker.LocalBinder) service).getService();
			// Tell the user about this for our demo.
			StartFragment.this.onGpsTrackerBound();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mGpsTracker = null;
		}
	};

	void bindGpsTracker() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		getActivity().getApplicationContext().bindService(new Intent(getActivity(), GpsTracker.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void unbindGpsTracker() {
		if (mIsBound) {
			// Detach our existing connection.
			getActivity().getApplicationContext().unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			if (data.getStringExtra("url") != null)
				System.err.println("data.getStringExtra(\"url\") => "+ data.getStringExtra("url"));
			if (data.getStringExtra("ex") != null)
				System.err.println("data.getStringExtra(\"ex\") => " + data.getStringExtra("ex"));
			if (data.getStringExtra("obj") != null)
				System.err.println("data.getStringExtra(\"obj\") => " + data.getStringExtra("obj"));
		}
		if (requestCode == 112) {
			skipStopGps = false;
			if (mIsBound == false || mGpsTracker == null) {
				bindGpsTracker();
			} else {
				onGpsTrackerBound();
			}
		} else {
			updateView();
			advancedWorkoutListAdapter.reload();
		}
	}

	@Override
	public void onTick() {
		updateView();
	}

	OnSetValueListener simpleTypeSetValue = new OnSetValueListener() {

		@Override
		public String preSetValue(String newValue)
				throws IllegalArgumentException {
			return newValue;
		}

		@Override
		public int preSetValue(int newValue) throws IllegalArgumentException {
			boolean time = (newValue == 0);
			simpleTime.setVisibility(time ? View.VISIBLE : View.GONE);
			simpleDistance.setVisibility(time ? View.GONE : View.VISIBLE);
			return newValue;
		}
		
	};

	OnCheckedChangeListener simpleGoalOnCheckClick = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
			simpleType.setEnabled(isChecked);
			simpleTime.setEnabled(isChecked);
			simpleDistance.setEnabled(isChecked);
		}
	};

	OnCloseDialogListener simpleTargetTypeClick = new OnCloseDialogListener() {

		@Override
		public void onClose(TitleSpinner spinner, boolean ok) {
			if (ok) {
				updateTargetView();
			}
		}
	};

	void updateTargetView() {
		switch(simpleTargetType.getValueInt()) {
		case 0:
		default:
			simpleTargetPaceValue.setEnabled(false);
			simpleTargetHrz.setEnabled(false);
			break;
		case 1:
			simpleTargetPaceValue.setEnabled(true);
			simpleTargetPaceValue.setVisibility(View.VISIBLE);
			simpleTargetHrz.setVisibility(View.GONE);
			break;
		case 2:
			simpleTargetPaceValue.setVisibility(View.GONE);
			simpleTargetHrz.setEnabled(true);
			simpleTargetHrz.setVisibility(View.VISIBLE);
		}
	}
	
	OnSetValueListener intervalTypeSetValue = new OnSetValueListener() {

		@Override
		public String preSetValue(String newValue)
				throws IllegalArgumentException {
			return newValue;
		}

		@Override
		public int preSetValue(int newValue) throws IllegalArgumentException {
			boolean time = (newValue == 0);
			intervalTime.setVisibility(time ? View.VISIBLE : View.GONE);
			intervalDistance.setVisibility(time ? View.GONE : View.VISIBLE);
			return newValue;
		}
	};

	OnSetValueListener intervalRestTypeSetValue = new OnSetValueListener() {

		@Override
		public String preSetValue(String newValue)
				throws IllegalArgumentException {
			return newValue;
		}

		@Override
		public int preSetValue(int newValue) throws IllegalArgumentException {
			boolean time = (newValue == 0);
			intervalRestTime.setVisibility(time ? View.VISIBLE : View.GONE);
			intervalRestDistance.setVisibility(time ? View.GONE : View.VISIBLE);
			return newValue;
		}
	};

	
	void loadAdvanced(String name) {
		Context ctx = getActivity().getApplicationContext();
		if (name == null) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
			name = pref.getString("advancedWorkout", "");
		}
		advancedWorkout = null;
		if ("".contentEquals(name))
			return;
		try {
			advancedWorkout = WorkoutSerializer.readFile(ctx, name);
			advancedWorkoutStepsAdapter.steps = advancedWorkout.getSteps();
			advancedWorkoutStepsAdapter.notifyDataSetChanged();
			advancedDownloadWorkoutButton.setVisibility(View.GONE);
		} catch (Exception ex) {
			ex.printStackTrace();
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("Failed to load workout!!");
			builder.setMessage("" + ex.toString());
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.show();
			return;
		}
	}
	
	class WorkoutStepsAdapter extends BaseAdapter {

		List<StepListEntry> steps = new ArrayList<StepListEntry>();
		
		@Override
		public int getCount() {
			return steps.size();
		}

		@Override
		public Object getItem(int position) {
			return steps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			StepListEntry entry = steps.get(position);
			StepButton button = null;
			if (convertView != null && convertView instanceof StepButton) {
				button = (StepButton)convertView;
			} else {
				button = new StepButton(getActivity(), null);
			}
			button.setStep(entry.step);
			button.setPadding(entry.level * 7, 0, 0, 0);
			return button;
		}
	};
	
	OnSetValueListener onSetTimeValidator = new OnSetValueListener() {

		@Override
		public String preSetValue(String newValue)
				throws IllegalArgumentException {

			
			if (WorkoutBuilder.validateSeconds(newValue))
				return newValue;

			throw new IllegalArgumentException("Unable to parse time value: " + newValue);
		}

		@Override
		public int preSetValue(int newValue) throws IllegalArgumentException {
			return newValue;
		}
		
	};

	OnSetValueListener onSetValueManual = new OnSetValueListener() {

		@Override
		public String preSetValue(String newValue)
				throws IllegalArgumentException {
			manualSetValue = true;
			startButton.setEnabled(true);
			return newValue;
		}

		@Override
		public int preSetValue(int newValue) throws IllegalArgumentException {
			manualSetValue = true;
			startButton.setEnabled(true);
			return newValue;
		}
	};
	
	void setManualPace(String distance, String duration) {
		System.err.println("distance: >" + distance + "< duration: >" + duration + "<");
		double dist = SafeParse.parseDouble(distance, 0); // convert to meters
		long seconds = SafeParse.parseSeconds(duration, 0);
		if (dist == 0 || seconds == 0) {
			manualPace.setVisibility(View.GONE);
			return;
		}
		double pace = seconds / dist;
		manualPace.setValue(formatter.formatPace(Formatter.TXT_SHORT, pace));
		manualPace.setVisibility(View.VISIBLE);
		return;
	}
	
	OnSetValueListener onSetManualDistance = new OnSetValueListener() {

		@Override
		public String preSetValue(String newValue)
				throws IllegalArgumentException {
			setManualPace(newValue, manualDuration.getValue().toString());
			startButton.setEnabled(true);
			return newValue;
		}

		@Override
		public int preSetValue(int newValue) throws IllegalArgumentException {
			startButton.setEnabled(true);
			return newValue;
		}
		
	};

	OnSetValueListener onSetManualDuration = new OnSetValueListener() {

		@Override
		public String preSetValue(String newValue)
				throws IllegalArgumentException {
			setManualPace(manualDistance.getValue().toString(), newValue);
			startButton.setEnabled(true);
			return newValue;
		}

		@Override
		public int preSetValue(int newValue) throws IllegalArgumentException {
			startButton.setEnabled(true);
			return newValue;
		}
	};

	OnClickListener manualSaveButtonClick = new OnClickListener()  {

		@Override
		public void onClick(View v) {
			ContentValues save = new ContentValues();
			CharSequence date = manualDate.getValue();
			CharSequence time = manualTime.getValue();
			CharSequence distance = manualDistance.getValue();
			CharSequence duration = manualDuration.getValue();
			String notes = manualNotes.getText().toString().trim();
			long start_time = 0;
			
			if (notes.length() > 0) {
				save.put(DB.ACTIVITY.COMMENT, notes);
			}
			double dist = 0;
			if (distance.length() > 0) {
				dist = Double.parseDouble(distance.toString()); // convert to meters
				save.put(DB.ACTIVITY.DISTANCE, dist);
			}
			long secs = 0;
			if (duration.length() > 0) {
				secs = SafeParse.parseSeconds(duration.toString(), 0);
				save.put(DB.ACTIVITY.TIME, secs);
			}
			if (date.length() > 0) {
				DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
				try {
					Date d = df.parse(date.toString());
					start_time += d.getTime() / 1000;
				} catch (ParseException e) {
				}
			}
			if (time.length() > 0) {
				DateFormat df = android.text.format.DateFormat.getTimeFormat(getActivity());
				try {
					Date d = df.parse(time.toString());
					start_time += d.getTime() / 1000;
				} catch (ParseException e) {
				}
			}
			save.put(DB.ACTIVITY.START_TIME, start_time);

			long id = mDB.insert(DB.ACTIVITY.TABLE, null, save);

			ContentValues lap = new ContentValues();
			lap.put(DB.LAP.ACTIVITY, id);
			lap.put(DB.LAP.LAP, 0);
			lap.put(DB.LAP.INTENSITY, DB.INTENSITY.ACTIVE);
			lap.put(DB.LAP.TIME, secs);
			lap.put(DB.LAP.DISTANCE, dist);
			mDB.insert(DB.LAP.TABLE, null, lap);
			
			Intent intent = new Intent(getActivity(), DetailActivity.class);
			intent.putExtra("mode", "save");
			intent.putExtra("ID", id);
			StartFragment.this.startActivityForResult(intent, 0);
		}
	};

    class TargetEntriesAdapter extends BaseAdapter {

        String[] entries;
        LayoutInflater inflator;
        HashSet<String> disabled;

        TargetEntriesAdapter(Context ctx) {
            inflator = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            entries = ctx.getResources().getStringArray(R.array.targetEntries);
        }

        void addDisabled(int i) {
            if (disabled == null)
                disabled = new HashSet<String>();
            if (i < entries.length)
                disabled.add(entries[i]);
        }

        @Override
        public int getCount() {
            return entries.length;
        }

        @Override
        public Object getItem(int position) {
            if (position < entries.length)
                return entries[position];
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String str = (String) getItem(position);
            if (convertView == null) {
                convertView = inflator.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }
            TextView ret = (TextView) convertView.findViewById(android.R.id.text1);
            ret.setText(str);

            if (disabled != null && disabled.contains(str))
                convertView.setEnabled(false);
            else
                convertView.setEnabled(true);

            return convertView;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return disabled == null || disabled.size() == 0;
        }

        @Override
        public boolean isEnabled(int position) {
            if (disabled == null)
                return true;

            String str = (String) getItem(position);
            if (str == null)
                return true;

            if (disabled.contains(str))
                return false;

            return true;
        }
    };
}