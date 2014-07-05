package org.runnerup.pedometer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by niklas.weidemann on 2014-07-05.
 */
public class AccelerometerCadenceProvider implements CadenceProvider, OnStepsCountedListener {

	private final Context mContext;
	private StepCounterInteractor mStepServiceInteractor;
	private long mStepCount;
	private int mCadense;
	private int mAvgCadense;

	public AccelerometerCadenceProvider(Context context) {
		mContext = context;
	}
	@Override
	public void start() {
		startPedometerService();

	}

	@Override
	public void stop() {
		if(mStepServiceInteractor != null) {
			mStepServiceInteractor.unregisterOnStepsCountedListener(this);
			mContext.unbindService(mServiceConnection);
		}


	}

	@Override
	public int getCadenceValue() {
		return mCadense;
	}

	private void startPedometerService() {
		final Intent intent = new Intent(mContext, StepCounterService.class);
		mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		if(mStepServiceInteractor != null) {
			mStepServiceInteractor.registerOnStepsCountedListener(this);
			if(!mStepServiceInteractor.isListening())
				mStepServiceInteractor.startListening();
		}
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			final StepCounterService.StepServiceBinder binder = (StepCounterService.StepServiceBinder) service;
			mStepServiceInteractor = binder.getStepInteractor();
			mStepServiceInteractor.registerOnStepsCountedListener(AccelerometerCadenceProvider.this);
			mStepServiceInteractor.startListening();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mStepServiceInteractor = null;
		}
	};

	@Override
	public void onStepsCounted(StepCounterInteractor listener) {
		mStepCount = listener.getCountedSteps();
		mCadense = listener.getCadense();
		mAvgCadense = listener.getAvgCadense();
	}
}
