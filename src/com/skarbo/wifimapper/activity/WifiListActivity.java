package com.skarbo.wifimapper.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.skarbo.wifimapper.R;
import com.skarbo.wifimapper.handler.WifiHandler;
import com.skarbo.wifimapper.listener.WifiListener;
import com.skarbo.wifimapper.model.WifiScan;

public abstract class WifiListActivity extends ListActivity implements WifiListener {

	protected static final String TAG = "WifiListActivity";

	private static final int WIFI_LEVEL_HIGH = -60;
	private static final int WIFI_LEVEL_MEDIUM = -70;
	private static final int WIFI_LEVEL_LOW = -75;
	private static final int WIFI_LEVEL_LOWEST = -85;

	private static final Comparator<WifiScan> WIFISCAN_COMPARATOR = new Comparator<WifiScan>() {

		public int compare(WifiScan lhs, WifiScan rhs) {
			return Integer.valueOf(rhs.getScanResult().level).compareTo(Integer.valueOf(lhs.getScanResult().level));
			// return
			// rhs.getScanResult().SSID.compareTo(lhs.getScanResult().SSID);
		}
	};

	protected WifiHandler wifiHandler;
	protected WifiAdapter adapter;

	private boolean isInit = false;
	private List<WifiScan> scanResults;

	private WifiPresenter presenter;

	// ... ON

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wifi);

		if (!this.isInit) {
			doInit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		this.wifiHandler.doWifiUnlisten();
		this.isInit = false;
	}

	// ... /ON

	// ... GET

	public Context getContext() {
		return this.getApplicationContext();
	}

	private int getWifiIcon(ScanResult scanResult) {
		if (scanResult.level > WIFI_LEVEL_HIGH) {
			return R.drawable.wifi_4;
		} else if (scanResult.level > WIFI_LEVEL_MEDIUM) {
			return R.drawable.wifi_3;
		} else if (scanResult.level > WIFI_LEVEL_LOW) {
			return R.drawable.wifi_2;
		} else if (scanResult.level > WIFI_LEVEL_LOWEST) {
			return R.drawable.wifi_1;
		} else {
			return R.drawable.wifi_0;
		}
	}

	protected List<WifiScan> getScanResults() {
		return scanResults;
	}

	protected WifiAdapter getAdapter() {
		return adapter;
	}

	// ... /GET

	// ... IS

	protected abstract boolean isShowTag();

	// ... /IS

	// ... DO

	private void doInit() {
		// Init scan results
		this.scanResults = new ArrayList<WifiScan>();

		// Init adapter
		this.adapter = new WifiAdapter(this, R.layout.wifi_row, this.scanResults);

		// Set adapter sort
		this.adapter.sort(WIFISCAN_COMPARATOR);

		setListAdapter(this.adapter);

		// Init Wifi handler
		this.wifiHandler = new WifiHandler(this);

		// Listen to WiFi
		this.wifiHandler.doWifiListen();

		// Presenter
		this.presenter = new WifiPresenter();

		// Update connected to
		this.presenter.updateConnectedTo();

		this.isInit = true;
	}

	// ... /DO

	// ... HANDLE

	public void handleWifiScan(List<WifiScan> wifiScans) {
		// Set scan results
		this.scanResults = wifiScans;

		// Sort wifi scans
		Collections.sort(this.scanResults, WIFISCAN_COMPARATOR);

		// Remove all from adapter
		this.adapter.clear();

		// Add Scan Result to adapter
		for (WifiScan scanResult : wifiScans) {
			this.adapter.add(scanResult);
		}

		// Update searching
		this.presenter.updateSearching(this.adapter.isEmpty());

		// Notify data change
		this.adapter.notifyDataSetChanged();

		// Update connected to
		this.presenter.updateConnectedTo();
	}

	// ... /HANDLE

	// CLASS

	private class WifiPresenter {

		private TextView connectedToSSID;
		private TextView connectedToBSSID;
		private ImageView connectedToRouter;
		private LinearLayout searching;
		private ListView listView;
		private EditText filterEditText;

		public WifiPresenter() {
			this.connectedToSSID = (TextView) findViewById(R.id.wifi_connectedto_ssid);
			this.connectedToBSSID = (TextView) findViewById(R.id.wifi_connectedto_bssid);
			this.searching = (LinearLayout) findViewById(R.id.wifi_searching);
			this.connectedToRouter = (ImageView) findViewById(R.id.wifi_connectedto_router);
			this.filterEditText = (EditText) findViewById(R.id.wifi_filter);
			this.listView = getListView();

			// Add text change listener to filter text
			this.filterEditText.addTextChangedListener(new TextWatcher() {

				public void onTextChanged(CharSequence s, int start, int before, int count) {
					adapter.getFilter().filter(s);
				}

				public void beforeTextChanged(CharSequence s, int start, int count, int after) {

				}

				public void afterTextChanged(Editable s) {

				}
			});
		}

		public void updateConnectedTo() {
			if (wifiHandler.getWifiManager().isWifiEnabled()) {
				if (wifiHandler.getWifiManager().getConnectionInfo() != null) {
					this.connectedToSSID.setText(wifiHandler.getWifiManager().getConnectionInfo().getSSID());
					this.connectedToBSSID.setText(wifiHandler.getWifiManager().getConnectionInfo().getBSSID());
					this.connectedToBSSID.setVisibility(View.VISIBLE);
					this.connectedToRouter.setImageResource(R.drawable.router);
				} else {
					this.connectedToSSID.setText(getString(R.string.wifi_connectedto_none));
					this.connectedToBSSID.setVisibility(View.GONE);
					this.connectedToRouter.setImageResource(R.drawable.router_none);
				}
			} else {
				this.connectedToSSID.setText(getString(R.string.wifi_connectedto_notenabled));
				this.connectedToBSSID.setVisibility(View.GONE);
				this.connectedToRouter.setImageResource(R.drawable.router_none);
			}
		}

		public void updateSearching(boolean listEmpty) {
			if (listEmpty) {
				this.listView.setVisibility(View.GONE);
				this.searching.setVisibility(View.VISIBLE);
				this.filterEditText.setVisibility(View.GONE);
			} else {
				this.listView.setVisibility(View.VISIBLE);
				this.searching.setVisibility(View.GONE);
				this.filterEditText.setVisibility(View.VISIBLE);
			}
		}

	}

	protected class WifiAdapter extends ArrayAdapter<WifiScan> implements Filterable {

		private List<WifiScan> scanResults;
		private List<WifiScan> wifiScans;
		private Filter filter;
		private final Object lock = new Object();

		public WifiAdapter(Context context, int textViewResourceId, List<WifiScan> wifiScans) {
			super(context, textViewResourceId, wifiScans);
			this.wifiScans = wifiScans;
			this.scanResults = wifiScans;
			getListView().setTextFilterEnabled(true);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			if (view == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(R.layout.wifi_row, null);
			}

			WifiScan wifiScan = this.scanResults.size() > position && this.scanResults.size() >= 0 ? this.scanResults.get(position) : null;
			if (wifiScan != null) {
				ImageView signalImageView = (ImageView) view.findViewById(R.id.wifi_row_icon);
				ImageView connectedImageView = (ImageView) view.findViewById(R.id.wifi_row_connected);
				ImageView passwordImageView = (ImageView) view.findViewById(R.id.wifi_row_password);
				TextView ssidTextView = (TextView) view.findViewById(R.id.wifi_row_ssid);
				TextView bssidTextView = (TextView) view.findViewById(R.id.wifi_row_bssid);
				TextView levelTextView = (TextView) view.findViewById(R.id.wifi_row_level);
				TextView frequencyTextView = (TextView) view.findViewById(R.id.wifi_row_frequency);
				LinearLayout tagLinearLayout = (LinearLayout) view.findViewById(R.id.wifi_row_tag);
				TextView tagTextView = (TextView) view.findViewById(R.id.wifi_row_tag_textview);

				signalImageView.setImageResource(getWifiIcon(wifiScan.getScanResult()));
				ssidTextView.setText(wifiScan.getScanResult().SSID);
				bssidTextView.setText(wifiScan.getScanResult().BSSID);
				levelTextView.setText("" + wifiScan.getScanResult().level);
				frequencyTextView.setText("" + wifiScan.getScanResult().frequency);
				tagLinearLayout.setVisibility(isShowTag() && !wifiScan.getTag().equalsIgnoreCase("") ? View.VISIBLE
						: View.GONE);
				tagTextView.setText(wifiScan.getTag());

				// CONNECTED

				WifiInfo connectionInfo = wifiHandler.getWifiManager().getConnectionInfo();
				boolean isConnected = connectionInfo != null ? wifiScan.getScanResult().BSSID
						.equalsIgnoreCase(connectionInfo.getBSSID()) : false;

				if (isConnected) {
					connectedImageView.setVisibility(View.VISIBLE);
				} else {
					connectedImageView.setVisibility(View.GONE);
				}

				// /CONNECTED

				// PASSWORD

				if (wifiScan.getScanResult().capabilities.contains("WEP")
						|| wifiScan.getScanResult().capabilities.contains("WPA")) {
					passwordImageView.setVisibility(View.VISIBLE);
				} else {
					passwordImageView.setVisibility(View.GONE);
				}

				// /PASSWORD

			}

			return view;
		}

		@Override
		public Filter getFilter() {
			if (this.filter == null) {
				this.filter = new WifiFilter();
			}
			return filter;
		}

		// CLASS

		private class WifiFilter extends Filter {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();

				if (scanResults == null) {
					synchronized (lock) {
						scanResults = new ArrayList<WifiScan>(wifiScans);
					}
				}

				if (constraint == null || constraint.length() == 0) {
					synchronized (lock) {
						List<WifiScan> list = new ArrayList<WifiScan>(wifiScans);
						results.values = list;
						results.count = list.size();
					}
				} else {
					String filter = constraint.toString().toLowerCase();

					final List<WifiScan> values = scanResults;
					final int count = values.size();

					final List<WifiScan> newValues = new ArrayList<WifiScan>(count);

					for (WifiScan value : values) {
						if (value.getScanResult().BSSID.toLowerCase().contains(filter)
								|| value.getScanResult().SSID.toLowerCase().contains(filter)
								|| value.getTag().toLowerCase().contains(filter)) {
							newValues.add(value);
						}
					}

					results.values = newValues;
					results.count = newValues.size();
				}

				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				scanResults = (List<WifiScan>) results.values;
				if (results.count > 0) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}

		}

		// /CLASS

	}

	// /CLASS

}
