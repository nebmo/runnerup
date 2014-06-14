/*
 * Copyright (C) 2012 - 2013 jonas.oreland@gmail.com
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
package org.runnerup.view;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;

import org.runnerup.R;
import org.runnerup.db.DBHelper;
import org.runnerup.drawer.DrawerItem;
import org.runnerup.util.Constants.DB;
import org.runnerup.util.FileUtil;
import org.runnerup.util.Formatter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainLayout extends ActionBarActivity {
    private ActionBarDrawerToggle drawerToggle;
    private enum UpgradeState { UNKNOWN, NEW, UPGRADE, DOWNGRADE, SAME };
    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private int title;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        getSupportActionBar().setTitle(R.string.app_name);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        final List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
        drawerItems.add(new DrawerItem(R.string.nav_start, R.drawable.ic_tab_main, StartFragment.class));
        drawerItems.add(new DrawerItem(R.string.nav_feed, R.drawable.ic_tab_feed, FeedFragment.class));
        drawerItems.add(new DrawerItem(R.string.nav_history, R.drawable.ic_tab_history, HistoryFragment.class));
        drawerItems.add(new DrawerItem(R.string.nav_settings, R.drawable.ic_tab_setup, SettingsFragment.class));
        drawerList.setAdapter(new NavDrawerAdapter(this, drawerItems));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DrawerItem item = (DrawerItem) parent.getItemAtPosition(position);
                if(item.getFragmentClass().equals(SettingsFragment.class)) {
                    startActivity(new Intent(MainLayout.this, SettingsActivity.class));
                    drawerLayout.closeDrawer(drawerList);
                    return;
                }

                selectItem(position, item);
            }
        });

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(title);
                supportInvalidateOptionsMenu();
            }
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(R.string.app_name);
                supportInvalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        selectItem(0, drawerItems.get(0));

        initPreferences();
	}

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position, DrawerItem item) {
        // Create a new fragment and specify the planet to show based on position
        Fragment fragment = null;
        try {
            fragment = item.getFragmentClass().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.main_content, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        drawerList.setItemChecked(position, true);
        setTitle(item.getTitleId());
        drawerLayout.closeDrawer(drawerList);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);

        this.title = titleId;
        getSupportActionBar().setTitle(titleId);
    }

    private void initPreferences() {
        int versionCode = 0;
        UpgradeState upgradeState = UpgradeState.UNKNOWN;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = pref.edit();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pInfo.versionCode;
            int version = pref.getInt("app-version", -1);
            if (version == -1) {
                upgradeState = UpgradeState.NEW;
            } else if (versionCode == version) {
                upgradeState = UpgradeState.SAME;
            } else if (versionCode > version) {
                upgradeState = UpgradeState.UPGRADE;
            } else if (versionCode < version) {
                upgradeState = UpgradeState.DOWNGRADE;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        editor.putInt("app-version", versionCode);
        boolean km = Formatter.getUseKilometers(pref, editor);

        if (upgradeState == UpgradeState.NEW) {
            editor.putString(getResources().getString(R.string.pref_autolap),
                    Double.toString(km ? Formatter.km_meters : Formatter.mi_meters));
        }
        editor.commit();

        // clear basicTargetType between application startup/shutdown
        pref.edit().remove("basicTargetType").commit();

        System.err.println("app-version: " + versionCode + ", upgradeState: " + upgradeState + ", km: " + km);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        PreferenceManager.setDefaultValues(this, R.xml.audio_cue_settings, true);

        if (upgradeState == UpgradeState.UPGRADE) {
            whatsNew();
        }

        handleBundled(getApplicationContext().getAssets(), "bundled", getFilesDir().getPath()+"/..");
    }

	void handleBundled (AssetManager mgr, String src, String dst) {
		String list[] = null;
		try {
			list = mgr.list(src);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (list != null) {
			for (int i = 0; i < list.length; ++i) {
				boolean isFile = false;
				String add = list[i];
				try {
					InputStream is = mgr.open(src + File.separator + add);
					is.close();
					isFile = true;
				} catch (Exception ex) {
				}

				System.err.println("Found: " + dst + ", " + add + ", isFile: " + isFile);
				if (isFile == false) {
					File dstDir = new File(dst + File.separator + add);
					dstDir.mkdir();
					if (!dstDir.isDirectory()) {
						System.err.println("Failed to copy " + add + " as \"" + dst + "\" is not a directory!");
						continue;
					}
					if (dst == null)
						handleBundled(mgr, src + File.separator + add, add);
					else
						handleBundled(mgr, src + File.separator + add, dst + File.separator + add);
				} else {
					String tmp = dst + File.separator + add;
					File dstFile = new File(tmp);
					if (dstFile.isDirectory() || dstFile.isFile()) {
						System.err.println("Skip: " + tmp + 
								", isDirectory(): " + dstFile.isDirectory() +
								", isFile(): " + dstFile.isFile());
						continue;
					}

					String key = "install_bundled_"+add;
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
					if (pref.contains(key)) {
						System.err.println("Skip: " + key);
						continue;
						
					}
					
					pref.edit().putBoolean(key, true).commit();					
					System.err.println("Copying: " + tmp);
					InputStream input = null;
					try {
						input = mgr.open(src + File.separator + add);
						FileUtil.copy(input, tmp);
						handleHooks(src, add);
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						FileUtil.close(input);
					}
				}
			}
		}
	}
	
	private void handleHooks(String path, String file) {
		if (file.contains("_audio_cues.xml")) {
			String name = file.substring(0, file.indexOf("_audio_cues.xml"));

			DBHelper mDBHelper = new DBHelper(this);
			SQLiteDatabase mDB = mDBHelper.getWritableDatabase();

			ContentValues tmp = new ContentValues();
			tmp.put(DB.AUDIO_SCHEMES.NAME, name);
			tmp.put(DB.AUDIO_SCHEMES.SORT_ORDER, 0);
			mDB.insert(DB.AUDIO_SCHEMES.TABLE, null, tmp);
			
			mDB.close();
			mDBHelper.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        //boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);

        return super.onPrepareOptionsMenu(menu);
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

		switch (item.getItemId()) {
		case R.id.menu_accounts:
            startActivity(new Intent(this, AccountListActivity.class));
			break;
		case R.id.menu_workouts:
            startActivity(new Intent(this, ManageWorkoutsActivity.class));
			break;
		case R.id.menu_audio_cues:
            startActivity(new Intent(this, AudioCueSettingsActivity.class));
			break;
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.menu_rate:
			onRateClick.onClick(null);
			break;
		case R.id.menu_whatsnew:
			whatsNew();
			break;
		}
		return true;
	}
	
	public OnClickListener onRateClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			try {
			Uri uri = Uri.parse("market://details?id=" + getPackageName());
		    startActivity(new Intent(Intent.ACTION_VIEW, uri));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	};
	
	public void whatsNew() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Service.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.whatsnew, null);
		WebView wv = (WebView) view.findViewById(R.id.webView1);
		builder.setTitle("What's new");
		builder.setView(view);
		builder.setPositiveButton("Rate RunnerUp", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onRateClick.onClick(null);
			}
			
		});
		builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
		wv.loadUrl("file:///android_asset/changes.html");
	}
}