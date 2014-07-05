package org.runnerup.pedometer;

import android.content.Context;

/**
 * Created by niklas.weidemann on 2014-07-05.
 */
public interface CadenceProvider {
	public abstract void start();

	public abstract void stop();

	public abstract int getCadenceValue();
}
