package com.skarbo.wifimapper.handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import android.net.wifi.ScanResult;
import android.os.Environment;

import com.skarbo.wifimapper.model.WifiMap;
import com.skarbo.wifimapper.model.WifiMapper;
import com.skarbo.wifimapper.model.WifiScan;

public class SDHandler {

	private static String DIRECTORY_ROOT = "wifimapper";
	private static String DIRECTORY_MAPPER = "mapper";
	private static String FILE_EXTENTION = "txt";
	private static String FILE_TAGGER = "tagger.%s";
	/** "session_id_time" */
	private static String FILE_MAPPER = "%d_%d_%s.%s";

	private File taggerFile;
	private File mapperDirectory;
	private File mapperFile;
	private Properties taggerProperties;
	private boolean isTaggerLoaded = false;

	public SDHandler() {
		this.taggerFile = new File(getDirectory(), String.format(FILE_TAGGER, FILE_EXTENTION));
		this.mapperDirectory = new File(getMapperDirectory());
		this.taggerProperties = new Properties();
	}

	// ... DO

	private void doLoadTagger() throws FileNotFoundException, IOException {
		if (!this.isTaggerLoaded) {
			// Create tagger file
			this.doCreateFile(this.taggerFile);

			// Load tagger properties
			this.taggerProperties.load(new FileInputStream(this.taggerFile));

			this.isTaggerLoaded = true;
		}
	}

	private void doStoreTagger() throws FileNotFoundException, IOException {
		this.taggerProperties.store(new FileOutputStream(this.taggerFile), null);
	}

	private void doCreateFile(File file) throws IOException {
		if (!file.exists()) {
			File folder = file.getParentFile();
			if (!folder.exists()) {
				folder.mkdirs();
			}
			file.createNewFile();
		}
	}

	private void doWriteToFile(File file, String string) throws IOException {
		FileWriter fstream = new FileWriter(file, true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(string);
		out.close();
	}

	// ... /DO

	// ... GET

	private String getDirectory() {
		return String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), DIRECTORY_ROOT);
	}

	private String getMapperDirectory() {
		return String.format("%s/%s", getDirectory(), DIRECTORY_MAPPER);
	}

	public int getMapperDirectoryCount() {
		return mapperDirectory.exists() ? mapperDirectory.list().length : 0;
	}

	public List<String> getTags() throws FileNotFoundException, IOException {
		this.doLoadTagger();

		List<String> tags = new ArrayList<String>();
		String value = "";
		for (Object key : this.taggerProperties.keySet()) {
			value = this.taggerProperties.getProperty((String) key);
			tags.add(String.format("%s (%s)", value, (String) key));
		}

		return tags;
	}

	public String getTag(ScanResult scanResult) throws FileNotFoundException, IOException {
		this.doLoadTagger();
		return this.taggerProperties.getProperty(scanResult.BSSID, "");
	}

	private long getTimestamp() {
		return System.currentTimeMillis() / 1000;
	}

	// ... /GET

	// ... ADD

	public void addTag(String tag, ScanResult scanResult) throws FileNotFoundException, IOException {
		this.doLoadTagger();
		this.taggerProperties.setProperty(scanResult.BSSID, tag);
		this.doStoreTagger();
	}

	public void addScanToFile(WifiMap wifiMap) throws Exception {
		StringBuilder string = new StringBuilder();

		// Append map
		string.append(String.format("$%d|%d\n", wifiMap.count, getTimestamp()));

		// Append sensors
		string.append(String.format("?%s\n", Arrays.toString(wifiMap.sensors)));

		// For each WiFi scan
		for (WifiScan wifiScan : wifiMap.scans) {
			// Append WiFi scan
			string.append(String.format("%%%s|%s|%s|%s\n", wifiScan.getScanResult().SSID,
					wifiScan.getScanResult().BSSID, wifiScan.getScanResult().level, wifiScan.getScanResult().frequency));
		}

		// Write to file
		doWriteToFile(this.mapperFile, string.toString());
	}

	// ... /ADD

	// ... REMOVE

	public void removeTag(ScanResult scanResult) throws FileNotFoundException, IOException {
		this.doLoadTagger();
		this.taggerProperties.remove(scanResult.BSSID);
		this.doStoreTagger();
	}

	// ... REMOVE

	// ... CREATE

	public void createMapperFile(WifiMapper mapper) throws Exception {
		// Create date time string
		String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();

		// Create mapper file name
		String fileName = String.format(FILE_MAPPER, mapper.session, mapper.id, dateTime, FILE_EXTENTION);

		// Create mapper file
		this.mapperFile = new File(getMapperDirectory(), fileName);

		// Create mapper file
		doCreateFile(this.mapperFile);

		// Create first line
		String firstLine = String.format("#%d|%d|%s|%d|%d|%s|%s\n", mapper.session, mapper.id, getTimestamp(),
				mapper.scans, mapper.delay, mapper.connected != null ? mapper.connected.getBSSID() : "", mapper.device);

		// Write to file
		doWriteToFile(this.mapperFile, firstLine);
	}

	// ... /CREATE

}
