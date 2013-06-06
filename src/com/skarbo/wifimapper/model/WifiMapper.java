package com.skarbo.wifimapper.model;

import android.net.wifi.WifiInfo;

public class WifiMapper {

	public int session = 0;
	public int id = 0;
	public int scans = 0;
	public int delay = 0;
	public WifiInfo connected;
	public String device;

	public WifiMapper(int session, int id, int scans, int delay, WifiInfo connected, String device) {
		this.session = session;
		this.id = id;
		this.scans = scans;
		this.delay = delay;
		this.connected = connected;
		this.device = device;
	}

}
