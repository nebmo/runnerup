/*
 * Copyright (C) 2012 - 2013 jonas.oreland@gmail.com, weides@gmail.com
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

package org.runnerup.workout;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;

import org.runnerup.common.Constants;

@TargetApi(Build.VERSION_CODES.FROYO)
public class HeadsetButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (KeyEvent.ACTION_DOWN == event.getAction()) {
                Intent startBroadcastIntent = new Intent();
                startBroadcastIntent.setAction(Constants.Intents.START_STOP);
                context.sendBroadcast(startBroadcastIntent);
            }
        }
    }

}
