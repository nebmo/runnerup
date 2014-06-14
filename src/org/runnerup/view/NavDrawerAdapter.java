package org.runnerup.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.runnerup.R;
import org.runnerup.drawer.DrawerItem;

import java.util.List;

public class NavDrawerAdapter extends BaseAdapter {
    private final List<DrawerItem> drawerItems;
    private final Context context;

    public NavDrawerAdapter(Context context, List<DrawerItem> drawerItems) {
        this.context = context;
        this.drawerItems = drawerItems;
    }

    @Override
    public int getCount() {
        return drawerItems.size();
    }

    @Override
    public DrawerItem getItem(int position) {
        return drawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.drawer_list_item, parent, false);
            textView = (TextView) convertView.findViewById(R.id.textTitle);
            convertView.setTag(textView);
        }
        else {
            textView = (TextView) convertView.getTag();
        }

        final DrawerItem item = getItem(position);
        textView.setText(item.getTitleId());
        final Drawable icon = context.getResources().getDrawable(item.getIconId());
        final int bounds = context.getResources().getDimensionPixelSize(R.dimen.drawer_icon_size);
        icon.setBounds(0, 0, bounds, bounds);
        textView.setCompoundDrawables(icon, null, null, null);

        return convertView;
    }
}
