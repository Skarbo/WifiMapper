package com.skarbo.wifimapper.listener;

import java.util.List;

import android.content.Context;

import com.skarbo.wifimapper.model.WifiScan;

public interface WifiListener {

	abstract Context getContext();

	abstract void handleWifiScan(List<WifiScan> wifiScans);

	abstract void handleWifiTick(long progress, long max);

}
