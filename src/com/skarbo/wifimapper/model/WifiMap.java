package com.skarbo.wifimapper.model;

import java.util.List;

public class WifiMap {

	public WifiMapper mapper;
	public int count;
	public float[] sensors;
	public List<WifiScan> scans;

	public WifiMap(WifiMapper mapper, int count, float[] sensors, List<WifiScan> scans) {
		super();
		this.mapper = mapper;
		this.count = count;
		this.sensors = sensors;
		this.scans = scans;
	}

}
