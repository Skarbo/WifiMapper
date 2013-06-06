package com.skarbo.wifimapper.model;

import android.net.wifi.ScanResult;

public class WifiScan {

	private ScanResult scanResult;
	private String tag;

	public WifiScan(String tag, ScanResult scanResult) {
		this.tag = tag;
		this.scanResult = scanResult;
	}

	public ScanResult getScanResult() {
		return scanResult;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
