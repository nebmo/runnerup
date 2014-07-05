package org.runnerup.pedometer;

//http://www.analog.com/static/imported-files/tech_articles/pedometer.pdf
public class Pedometer extends StepsCounterBase implements IPedometer {
	private double[] mNewValue = new double[4];
	private double[] mOldValue = new double[4];
	private int mCount;
	private double mMinValue;
	private double mMaxValue;
	private double mThreshold;
	private static final double PRECISION = 0.1d;
	private long lastStep;

	@Override
	public void onInput(AccelerationInfo info) {
		double value = info.wx;
		if (mNewValue[0] > mMaxValue)
			mMaxValue = mNewValue[0];
		if (mNewValue[0] < mMinValue)
			mMinValue = mNewValue[0];

		mCount++;
		if (mCount == 25) {
			mThreshold = (mMinValue + mMaxValue) / 2;
			mMinValue = 0;
			mMaxValue = 0;
			mCount = 0;
		}
		mOldValue[0] = mNewValue[0];
		double diff = Math.abs(value - mNewValue[0]);
		if (diff > PRECISION) {
			mNewValue[0] = value;
		}
		if (mOldValue[0] > mThreshold && mThreshold > mNewValue[0]) {
			long timediff = info.time - lastStep;
			if (timediff > 200) {
				lastStep = info.time;
				addStep(info.time);
			}
		}

	}

	@Override
	public int getSteps() {
		return mSteps;
	}

	@Override
	public int getCadense() {
		return super.getCadense();
	}
}

