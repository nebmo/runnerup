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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.wearable.view.FragmentGridPagerAdapter;

public class RunnerUpGridPagerAdapter extends FragmentGridPagerAdapter {

    private final Context mContext;

    public RunnerUpGridPagerAdapter(Context ctx, FragmentManager fm) {
        super(fm);
        mContext = ctx;
    }

    /**
     * A simple container for static data in each page
     */
    private static class Page {
        int titleRes;

        public Page() {
        }

        public Page(int titleRes) {
            this.titleRes = titleRes;
        }
    }

    private final Page[][] PAGES = {
            {
                    new Page(R.string.stats_title_total),
                    new Page(),
            },
            {
                    new Page(R.string.stats_title_lap),
                    new Page(),
            },
            {
                    new Page(R.string.stats_title_interval),
                    new Page(),
            },

    };

    @Override
    public Fragment getFragment(int row, int col) {
        Page page = PAGES[row][col];
        String title = page.titleRes != 0 ? mContext.getString(page.titleRes) : null;
        Fragment fragment;
        if (col == 0) {
            fragment = RunInformationCardFragment.create(title);
        } else {
            fragment = PauseResumeCardFragment.create(R.string.pause);
        }
        return fragment;
    }

    @Override
    public int getRowCount() {
        return PAGES.length;
    }

    @Override
    public int getColumnCount(int rowNum) {
        return PAGES[rowNum].length;
    }
}
