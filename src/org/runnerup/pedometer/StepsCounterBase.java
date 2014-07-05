package org.runnerup.pedometer;

/**
 * Created by niklas.weidemann on 2014-06-23.
 */
public class StepsCounterBase implements IStepsCounter {
	protected long[] mLastSteps = new long[5];
	protected long mStartTime;
	protected int mLastStepIndex = -1;
	protected int mSteps;

	@Override
	public double getAvgTimeBetweenSteps() {
		if(mLastStepIndex == 4){
			return (mLastSteps[0]- mLastSteps[4]) / 4.0;
		}
		return 0;
	}

	@Override
	public int getCadense() {
		double avgTime = getAvgTimeBetweenSteps();
		if(avgTime == 0.0)
			return 0;
		return (int) Math.round((60 * 1000) / getAvgTimeBetweenSteps());
	}

	@Override
	public int getAvgCadense() {
		if(mSteps < 5)
			return 0;
		return (int) Math.round((60 * 1000) / ((mLastSteps[0] - mStartTime) / (mSteps - 1)));
	}

	public void addStep(long timestamp){
		if(mSteps == 0)
			mStartTime = timestamp;
		System.arraycopy(mLastSteps, 0, mLastSteps, 1, mLastSteps.length - 1);
		mLastSteps[0] = timestamp;
		mLastStepIndex = Math.min(mLastStepIndex + 1, 4);
		mSteps++;
	}
}
