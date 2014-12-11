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
package org.runnerup.common;

public class WearConstants {

    public static final String NOTIFICATION_PATH = "/notification";
    public static final String RUNNERUP_EVENT_PATH = "/event";
    public static final String NOTIFICATION_TIMESTAMP = "timestamp";
    public static final String NOTIFICATION_TITLE = "title";
    public static final String NOTIFICATION_CONTENT = "content";

    public static final String ACTION_DISMISS = "org.runnerup.DISMISS";

    public static final int TYPE_START = 1;
    public static final int TYPE_END = 2;
    public static final int TYPE_GPS = 3;
    public static final int TYPE_PAUSE = 4;
    public static final int TYPE_RESUME = 5;
    public static final int TYPE_DISCARD = 6;
}
