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
package org.runnerup.export;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.runnerup.common.WearConstants;
import org.runnerup.gpstracker.WorkoutProvider;
import org.runnerup.common.Constants;
import org.runnerup.util.Formatter;
import org.runnerup.workout.Intensity;
import org.runnerup.workout.Scope;
import org.runnerup.workout.Step;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutInfo;

public class WearLiveLogger implements LiveLogger {
    private final Context context;
    private final Formatter formatter;
    private GoogleApiClient googleApiClient;
    private int state = 0;

    public WearLiveLogger(Context context) {
        this.context = context;
        this.formatter = new Formatter(context);
        createGoogleApiClient();
    }

    private void createGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(context.getApplicationContext())
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d("WearLiveLogger", "onConnected: " + connectionHint);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d("WearLiveLogger", "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d("WearLiveLogger", "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void liveLog(WorkoutProvider workoutProvider, int type) {

        if (!googleApiClient.isConnected()) {
            Log.e("WearLiveLogger", "No connection to wearable available or no active workout");
            return;
        }
        PutDataMapRequest dataMapRequest = null;
        if(type == Constants.DB.LOCATION.TYPE_START || type == Constants.DB.LOCATION.TYPE_RESUME){
            dataMapRequest = PutDataMapRequest.create(WearConstants.NOTIFICATION_PATH);
            dataMapRequest.getDataMap().putDouble(WearConstants.NOTIFICATION_TIMESTAMP, System.currentTimeMillis());
            dataMapRequest.getDataMap().putString(WearConstants.NOTIFICATION_TITLE, "RunnerUp");
            dataMapRequest.getDataMap().putString(WearConstants.NOTIFICATION_CONTENT, "View Stats");
            dataMapRequest.getDataMap().putInt("activityStatus", type);
        } else if(workoutProvider.getWorkoutInfo() != null) {
            WorkoutInfo workout = workoutProvider.getWorkoutInfo();
            dataMapRequest = PutDataMapRequest.create(WearConstants.RUNNERUP_EVENT_PATH);
            dataMapRequest.getDataMap().putDouble(WearConstants.NOTIFICATION_TIMESTAMP, System.currentTimeMillis());
            double ad = workoutProvider.getWorkoutInfo().getDistance(Scope.WORKOUT);
            double at = workout.getTime(Scope.WORKOUT);
            double ap = workout.getPace(Scope.WORKOUT);
            double ah = workout.getHeartRate(Scope.WORKOUT);

            double sd = workout.getDistance(Scope.LAP);
            double st = workout.getTime(Scope.LAP);
            double sp = workout.getPace(Scope.LAP);
            double sh = workout.getHeartRate(Scope.LAP);
            boolean intervalActive = workout.getCurrentStep() != null && !isSimpleWorkOut(workout)
                    && workout.getCurrentStep().getIntensity() == Intensity.ACTIVE;
            double cd = workout.getDistance(Scope.STEP);
            double ct = workout.getTime(Scope.STEP);
            double cp = workout.getPace(Scope.STEP);
            double ch = workout.getHeartRate(Scope.STEP);

            String intervalInfo = "";
            if(intervalActive) {
                Step step = workout.getCurrentStep();
                if (step.getDurationType() != null) {
                    intervalInfo += context.getResources().getText(step.getDurationType().getTextId()) + " ";
                    intervalInfo += formatter.format(Formatter.TXT_LONG, step.getDurationType(), step.getDurationValue()) + " ";
                }

                if (step.getTargetType() != null) {
                    double minValue = step.getTargetValue().minValue;
                    double maxValue = step.getTargetValue().maxValue;
                    if (minValue == maxValue) {
                        intervalInfo += formatter.format(Formatter.TXT_SHORT, step.getTargetType(),
                                minValue) + " ";
                    } else {
                        intervalInfo += formatter.format(Formatter.TXT_SHORT, step.getTargetType(),
                                minValue) + "-" +
                                formatter.format(Formatter.TXT_SHORT, step.getTargetType(), maxValue) + " ";
                    }
                }
            }
            dataMapRequest.getDataMap().putString("Activity Time", formatter.formatElapsedTime(Formatter.GARMIN_NO_SUFFIX, Math.round(at)));
            dataMapRequest.getDataMap().putString("Activity Pace", formatter.formatPace(Formatter.GARMIN_NO_SUFFIX, ap));
            dataMapRequest.getDataMap().putString("Activity Distance", formatter.formatDistance(Formatter.GARMIN_NO_SUFFIX, Math.round(ad)));
            dataMapRequest.getDataMap().putString("Activity HR", formatter.formatHeartRate(Formatter.GARMIN_NO_SUFFIX, Math.round(ah)));
            dataMapRequest.getDataMap().putString("LAP Time", formatter.formatElapsedTime(Formatter.GARMIN_NO_SUFFIX, Math.round(st)));
            dataMapRequest.getDataMap().putString("LAP Pace", formatter.formatPace(Formatter.GARMIN_NO_SUFFIX, sp));
            dataMapRequest.getDataMap().putString("LAP Distance", formatter.formatDistance(Formatter.GARMIN_NO_SUFFIX, Math.round(sd)));
            dataMapRequest.getDataMap().putString("LAP HR", formatter.formatHeartRate(Formatter.GARMIN_NO_SUFFIX, Math.round(sh)));
            dataMapRequest.getDataMap().putBoolean("IntervalActive", intervalActive);
            dataMapRequest.getDataMap().putString("Step Time", formatter.formatElapsedTime(Formatter.GARMIN_NO_SUFFIX, Math.round(ct)));
            dataMapRequest.getDataMap().putString("Step Pace", formatter.formatPace(Formatter.GARMIN_NO_SUFFIX, cp));
            dataMapRequest.getDataMap().putString("Step Distance", formatter.formatDistance(Formatter.GARMIN_NO_SUFFIX, Math.round(cd)));
            dataMapRequest.getDataMap().putString("Step HR", formatter.formatHeartRate(Formatter.GARMIN_NO_SUFFIX, Math.round(ch)));
            dataMapRequest.getDataMap().putString("Step Info", intervalInfo);
            dataMapRequest.getDataMap().putInt("activityStatus", type);
        }
        state = type;
        if(dataMapRequest == null) return;

        PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, putDataRequest)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e("WearLiveLogger", "ERROR: failed to putDataItem, status code: "
                                    + dataItemResult.getStatus().getStatusCode());
                        }
                    }
                });
    }

    private boolean isSimpleWorkOut(WorkoutInfo workout) {
        return workout.getSteps().size() == 1 || workout.getSteps().size() == 2 && workout.getSteps().get(0).step.getIntensity() == Intensity.RESTING;
    }
}