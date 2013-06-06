package com.skarbo.wifimapper.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.util.Log;

import com.skarbo.wifimapper.listener.WifiListener;
import com.skarbo.wifimapper.model.WifiScan;

public class WifiHandler extends BroadcastReceiver {

	private static final String TAG = "WifiHandler";
	private static final String WIFI_TIMER = "wifi_timer";
	private static final int DELAY_DEFAULT = 5000;

	private WifiListener wifiListener;
	private WifiManager wifiManager;
	private ConnectivityManager connectivityManager;
	private SDHandler sdHandler;
	private int delay = DELAY_DEFAULT;
	private Timer wifiTimer;
	private CountDownTimer wifiCountDownTimer;

	public WifiHandler(final WifiListener wifiListener) {
		this.wifiListener = wifiListener;
		this.wifiManager = (WifiManager) this.wifiListener.getContext().getSystemService(Context.WIFI_SERVICE);
		this.connectivityManager = (ConnectivityManager) this.wifiListener.getContext().getSystemService(
				Context.CONNECTIVITY_SERVICE);
		this.sdHandler = new SDHandler();
		this.wifiTimer = new Timer(WIFI_TIMER);
		
		this.wifiCountDownTimer = new CountDownTimer(delay, 100) {
			
			@Override
			public void onTick(long millisUntilFinished) {
				wifiListener.handleWifiTick(millisUntilFinished, delay);
			}
			
			@Override
			public void onFinish() {
				try {
					doWifiScan(delay);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			}
		};
	}

	// ... GET

	public WifiManager getWifiManager() {
		return wifiManager;
	}

	public ConnectivityManager getConnectivityManager() {
		return connectivityManager;
	}

	public SDHandler getSdHandler() {
		return sdHandler;
	}

	public int getNextUniqueId() {
		return sdHandler.getMapperDirectoryCount() + 1;
	}

	// ... /GET

	// ... SET

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public void setDelayDefault() {
		this.delay = DELAY_DEFAULT;
	}

	// ... /SET

	// ... IS

	public boolean isWifiEnabled() {
		return wifiManager.isWifiEnabled();
	}

	public boolean isWifiConnected() {
		NetworkInfo connectivityNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return connectivityNetworkInfo.isConnected();
	}

	// ... /IS

	// ... DO

	public void doWifiListen() {
		// Intent filter
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

		// Register WiFi receiver
		this.wifiListener.getContext().registerReceiver(this, intentFilter);
	}

	public void doWifiUnlisten() {
		// Unregister WiFio receiver
		this.wifiListener.getContext().unregisterReceiver(this);
	}

	public void doWifiScan() throws Exception {
		doWifiScan(DELAY_DEFAULT);
	}

	public void doWifiScan(int delay) throws Exception {
		this.delay = delay;
		if (isWifiEnabled()) {
			if (isWifiConnected()) {
				this.wifiManager.startScan();
			} else {
				throw new Exception("Wifi is not connected");
			}
		} else {
			throw new Exception("Wifi is not enabled");
		}
	}

	// ... /DO

	// ... ON

	@Override
	public void onReceive(Context context, Intent intent) {
		if (this.wifiListener != null) {

			// Generate list of WiFi scan
			List<WifiScan> wifiScans = new ArrayList<WifiScan>();
			String tag = "";
			for (ScanResult scanResult : this.wifiManager.getScanResults()) {
				try {
					tag = this.sdHandler.getTag(scanResult);
				} catch (Exception e) {
					tag = "";
					Log.e(TAG, e.getMessage());
				}
				wifiScans.add(new WifiScan(tag, scanResult));
			}

			this.wifiListener.handleWifiScan(wifiScans);

			// Schedule new scan
//			wifiTimer.schedule(new TimerTask() {
//
//				@Override
//				public void run() {
//					try {
//						doWifiScan(delay);
//					} catch (Exception e) {
//						Log.e(TAG, e.getMessage());
//					}
//				}
//			}, delay);
			this.wifiCountDownTimer.start();
			
		}
	}

	// ... /ON

}
