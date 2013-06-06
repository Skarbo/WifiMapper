package com.skarbo.wifimapper.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.skarbo.wifimapper.R;
import com.skarbo.wifimapper.handler.WifiHandler;
import com.skarbo.wifimapper.listener.WifiListener;
import com.skarbo.wifimapper.model.WifiScan;

public class MainTabActivity extends TabActivity implements WifiListener {
	private static final String TAG = "MainTabActivity";
	private static final int DIALOG_TAGS = 1;

	private WifiHandler wifiHandler;

	private MainTabPresenter presenter;
	private TagsPresenter tagsPresenter;
	private boolean isInit = false;

	// ... ON

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On create");
		setContentView(R.layout.main_tab);

		if (!this.isInit) {
			doInit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		this.wifiHandler.doWifiUnlisten();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.wifi_menu_tags:
			showDialog(DIALOG_TAGS);
			break;

		default:
			break;
		}

		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_TAGS:
			List<String> tags = new ArrayList<String>();
			try {
				tags = this.wifiHandler.getSdHandler().getTags();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
			}
			this.tagsPresenter.updateTagsList(tags);
			return this.tagsPresenter.getAlertDialog();
		}

		return null;
	}

	// ... /ON

	// ... GET

	public Context getContext() {
		return super.getApplicationContext();
	}

	// ... /GET

	// ... DO

	private void doInit() {
		// Init presenter
		this.presenter = new MainTabPresenter(this.getApplicationContext());
		this.tagsPresenter = new TagsPresenter(this.getApplicationContext());

		this.wifiHandler = new WifiHandler(this);

		// Listen to WiFi
		this.wifiHandler.doWifiListen();

		// Start WiFi scan
		try {
			this.wifiHandler.doWifiScan();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}

		this.isInit = true;
	}

	// ... /DO

	// ... HANDLE

	public void handleWifiScan(List<WifiScan> wifiScans) {
		// Update WiFi tab
		this.presenter.updateWifiTabTitle(wifiScans.size());
		
		this.presenter.updateWifiProgress(0, 0);
	}

	public void handleWifiTick(long progress, long max) {
		// Update WiFi progress
		this.presenter.updateWifiProgress((int) (max - progress), (int) max);
	}

	// ... /HANDLE

	// CLASS

	private class MainTabPresenter {

		private static final String TAB_MAPPER = "mapper";
		private static final String TAB_WIFIS = "wifis";
		private TabHost tabHost;
		private Map<String, View> tabViews;
		private ProgressBar wifiScanProgress;

		public MainTabPresenter(Context context) {
			// Progress view
			this.wifiScanProgress = (ProgressBar) findViewById(R.id.wifi_scan_progress);
			
			// Setup tab host
			doSetupTabHost();

			// Set divider
			tabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);

			// Initiate tab views
			this.tabViews = new HashMap<String, View>();

			// Setup tabs
			doSetupTab(TAB_MAPPER, getString(R.string.mapper), new Intent(context, MapperActivity.class));
			doSetupTab(TAB_WIFIS, getString(R.string.wifis), new Intent(context, TaggerActivity.class));
			// doSetupTab(TAB_WIFIS, getString(R.string.wifis), new
			// Intent(context, WifiActivity.class));

			// Set "WiFi" as default tab
			tabHost.setCurrentTab(0);
		}

		// ... DO

		private void doSetupTabHost() {
			tabHost = (TabHost) findViewById(android.R.id.tabhost);
			tabHost.setup();
		}

		private void doSetupTab(final String tag, final String title, Intent intent) {
			View tabview = createTabView(tabHost.getContext(), title);

			TabSpec setContent = tabHost.newTabSpec(tag).setIndicator(tabview).setContent(intent);
			tabHost.addTab(setContent);

			// Add tab view
			this.tabViews.put(tag, tabview);
		}

		// ... /DO

		// ... CREATE

		private View createTabView(final Context context, final String text) {
			View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
			TextView tv = (TextView) view.findViewById(R.id.tabsText);
			tv.setText(text);
			return view;
		}

		// ... /CREATE

		// ... UPDATE

		public void updateWifiTabTitle(int count) {
			View tabview = this.tabViews.get(TAB_WIFIS);
			if (tabview != null) {
				TextView tabTitle = (TextView) tabview.findViewById(R.id.tabsText);
				if (tabTitle != null) {
					String title = String.format("%s (%d)", getString(R.string.wifis), count);
					tabTitle.setText(title);
				}
			}
		}
		
		public void updateWifiProgress(int progress, int max)
		{
			this.wifiScanProgress.setMax(max);
			this.wifiScanProgress.setProgress(progress);
		}

		// ... /UPDATE

	}

	private class TagsPresenter {

		private View dialogView;
		private AlertDialog alertDialog;
		private ListView tagsListView;
		private ArrayAdapter<String> tagsAdapter;
		private List<String> tagsList;

		public TagsPresenter(Context context) {
			LayoutInflater factory = LayoutInflater.from(MainTabActivity.this);
			this.tagsList = new ArrayList<String>();

			// Get dialog view
			this.dialogView = factory.inflate(R.layout.tagged_dialog, null);

			// Tags list
			this.tagsListView = (ListView) this.dialogView.findViewById(R.id.wifi_tags_list);
			this.tagsAdapter = new ArrayAdapter<String>(MainTabActivity.this, android.R.layout.simple_list_item_1,
					tagsList);
			this.tagsListView.setAdapter(this.tagsAdapter);

			// Create alert dialog
			this.alertDialog = new AlertDialog.Builder(MainTabActivity.this).setIcon(R.drawable.label)
					.setTitle(R.string.tags).setView(this.dialogView)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					}).create();

		}

		public AlertDialog getAlertDialog() {
			return this.alertDialog;
		}

		public void updateTagsList(List<String> tags) {
			if (this.tagsList != null) {
				this.tagsList.clear();
				this.tagsList.addAll(tags);
				Collections.sort(this.tagsList);
				this.tagsAdapter.notifyDataSetChanged();
			} else {
				Log.e(TAG, "TagsPresenter updateTagsList: tagsList is null");
			}
		}

	}

	// /CLASS

}
