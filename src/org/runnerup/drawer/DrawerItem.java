package org.runnerup.drawer;

import android.support.v4.app.Fragment;

public class DrawerItem {
    private final int titleId;
    private final int iconId;
    private final Class<? extends Fragment> fragmentClass;

    public DrawerItem(int titleId, int iconId, Class<? extends Fragment> fragmentClass) {
        this.titleId = titleId;
        this.iconId = iconId;
        this.fragmentClass = fragmentClass;
    }

    public int getTitleId() {
        return titleId;
    }

    public int getIconId() {
        return iconId;
    }

    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }
}
