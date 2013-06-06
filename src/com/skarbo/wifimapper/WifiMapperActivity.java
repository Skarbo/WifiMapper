package com.skarbo.wifimapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class WifiMapperActivity extends Activity {
	private static final String PREF_DELAY = "delay";
	private static final String PREF_SCANS = "scans";
	private static final String PREF_SESSION = "session";
	private static final String WIFI_TIMER = "wifi_timer";
	public static final long WIFI_SCAN_DELAY = 5000;
	private static final String TAG = "WifiMapper";
	private static final int SCANS_DEFAULT = 5;
	private static final int DELAY_DEFAULT = 5;
	private static final int SESSION_DEFAULT = 1;

	private WifiManager wifiManager;
	private Timer wifiTimer;
	private WifiBroadcastReceiver wifiScanReciver;
	private SensorManager sensorManager;
	private Sensor sensor;
	private ConnectivityManager connectivityManager;

	private ViewPresenter viewPresenter;
	private SensorListener sensorListener;
	private SDHandler sdHandler;
	private boolean isRegister = false;
	private int session = SESSION_DEFAULT;
	private int scans = SCANS_DEFAULT;
	private int delay = DELAY_DEFAULT;
	private int uniqueId = 0;
	private int registerCount = 0;
	private boolean isInit = false;
	private float[] sensors = new float[0];
	private SharedPreferences preferences;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On create");
		setContentView(R.layout.main);

		if (!isInit) {
			Log.d(TAG, "Initializing");

			preferences = PreferenceManager.getDefaultSharedPreferences(this);

			wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
			wifiTimer = new Timer(WIFI_TIMER);
			wifiScanReciver = new WifiBroadcastReceiver();

			connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

			sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			sensorListener = new SensorListener();

			viewPresenter = new ViewPresenter(this);
			sdHandler = new SDHandler();

			viewPresenter.setDevice(String.format("%s, %s", android.os.Build.MODEL, android.os.Build.VERSION.RELEASE));
			viewPresenter.setStatus("Ready");
			viewPresenter.setUniqueId("" + getNextUniqueId());
			viewPresenter.setSession("" + preferences.getInt(PREF_SESSION, SESSION_DEFAULT));
			viewPresenter.setScans("" + preferences.getInt(PREF_SCANS, SCANS_DEFAULT));
			viewPresenter.setDelay("" + preferences.getInt(PREF_DELAY, DELAY_DEFAULT));

			doStartWifi();
			doStartSensors();

			isInit = true;
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "On resume");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "On start");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "On pause");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "On stop");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "On destroy");
		doStopRegister();
		doStopWifi();
		doStopSensors();
		this.isInit = false;

		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(PREF_SESSION, this.session);
		editor.putInt(PREF_SCANS, this.scans);
		editor.putInt(PREF_DELAY, this.delay);
		editor.commit();
	}

	// ... GET

	public int getNextUniqueId() {
		return sdHandler.getMapperDirectoryCount() + 1;
	}

	// ... /GET

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

	public void doStartSensors() {
		Log.d(TAG, "Start sensor");
		if (sensor != null) {
			sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	public void doStopSensors() {
		sensorManager.unregisterListener(sensorListener);
	}

	public void doStartWifi() {
		Log.d(TAG, "Starting wifi");
		if (wifiManager.isWifiEnabled()) {
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			Log.d(TAG, "Register wifi reciever");
			this.registerReceiver(wifiScanReciver, intentFilter);
			wifiManager.startScan();
		} else {
			Log.d(TAG, "Wifi is not enabled");
		}
	}

	public void doStopWifi() {
		Log.d(TAG, "Stopping wifi");
		if (wifiManager.isWifiEnabled()) {
			Log.d(TAG, "Unregister wifi reciever");
			this.unregisterReceiver(wifiScanReciver);
		}
	}

	public void doStopRegister() {
		if (isRegister) {
			doNotify(String.format("Register #%d stopped, %d scan(s) registered", uniqueId, registerCount), true);

			isRegister = false;

			viewPresenter.setSessionEnabled(true);
			viewPresenter.setScansEnabled(true);
			viewPresenter.setDelayEnabled(true);
			viewPresenter.setRegisterEnabled(true);
			viewPresenter.setStopEnabled(false);
			viewPresenter.setStatus("Ready");
			viewPresenter.setUniqueId("" + getNextUniqueId());
		}
	}

	public void doStartRegister() throws Exception {
		if (this.isRegister) {
			throw new Exception("Register already in progess");
		}
		if (!isWifiEnabled()) {
			throw new Exception("Wifi is not enabled");
		}
		if (!isWifiConnected()) {
			throw new Exception("Wifi is not connected");
		}

		this.session = viewPresenter.getSession();
		this.scans = viewPresenter.getScans();
		this.delay = viewPresenter.getDelay();

		if (this.session < 1) {
			throw new Exception("Session must exceed 0");
		}
		if (this.scans < 1) {
			throw new Exception("Scans must exceed 0");
		}
		if (this.delay < 1) {
			throw new Exception("Delay must exceed 0");
		}

		this.uniqueId = getNextUniqueId();
		this.isRegister = true;
		this.registerCount = 0;

		WifiInfo connectionInfo = this.wifiManager.getConnectionInfo();

		this.viewPresenter.setSessionEnabled(false);
		this.viewPresenter.setDelayEnabled(false);
		this.viewPresenter.setScansEnabled(false);
		this.viewPresenter.setRegisterEnabled(false);
		this.viewPresenter.setStopEnabled(true);
		this.viewPresenter.setStatus("Register...");

		try {
			sdHandler.createMapperFile(this.session, this.uniqueId, this.scans, this.delay, connectionInfo.getBSSID(),
					String.format("%s, %s, %s", android.os.Build.MODEL, android.os.Build.VERSION.RELEASE,
							android.os.Build.BOOTLOADER));
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			doStopRegister();
			throw new Exception("Can't create log file");
		}

		doNotify(String.format("Starting register #%d with %d scan(s) and %d sec. delay", this.uniqueId, this.scans,
				this.delay), true);
	}

	public void doNotify(String message) {
		doNotify(message, false);
	}

	public void doNotify(String message, boolean longMessage) {
		(Toast.makeText(this, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
		Log.d(TAG, "Notify: " + message);
	}

	// ... /DO

	// ... HANDLE

	public void handleRecievedWifiScan(List<ScanResult> scanResults) {
		if (this.isRegister) {
			try {
				this.registerCount++;
				sdHandler.addScanToFile(this.registerCount, scanResults, this.sensors);
				viewPresenter.setStatus(String.format("Register %s/%s scans", this.registerCount, this.scans));
				Log.d(TAG, "Handled wifi scan: #" + uniqueId + ", count: " + this.registerCount + " scans: "
						+ scanResults.size() + ", sensors: " + this.sensors.length);

				if (registerCount >= scans) {
					doStopRegister();
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
				doNotify("Error while adding scan to file");
				doStopRegister();
			}
		}

		this.viewPresenter.doUpdateWifiCount(scanResults.size());

		Map<String, List<ScanResult>> scanResultsMap = generateScanResultsMap(scanResults);
		this.viewPresenter.doUpdateWifiTable(scanResultsMap);
	}

	public void handleRecievedSensor(float[] values) {
		sensors = values;
		viewPresenter.doUpdateDirection((float) values[0]);
	}

	public void handleRegister() {
		try {
			doStartRegister();
		} catch (Exception e) {
			doNotify(e.getMessage());
		}
	}

	public void handleStop() {
		doStopRegister();
	}

	// ... /HANDLE

	private static Map<String, List<ScanResult>> generateScanResultsMap(List<ScanResult> scanResults) {
		Map<String, List<ScanResult>> scanResultsMap = new TreeMap<String, List<ScanResult>>();
		for (ScanResult scanResult : scanResults) {
			if (!scanResultsMap.containsKey(scanResult.SSID)) {
				scanResultsMap.put(scanResult.SSID, new ArrayList<ScanResult>());
			}
			scanResultsMap.get(scanResult.SSID).add(scanResult);
		}
		return scanResultsMap;
	}

	class WifiBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			handleRecievedWifiScan(wifiManager.getScanResults());

			if (isWifiConnected()) {
				wifiTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						wifiManager.startScan();
					}
				}, delay * 1000);
			}
		}

	}

	class SensorListener implements SensorEventListener {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void onSensorChanged(SensorEvent event) {
			if (event.values.length > 0) {
				handleRecievedSensor(event.values);
			}
		}

	}

	class SDHandler {

		File directory;
		File mapperDirectory;
		File mapperFile;
		File taggerFile;
		Properties taggerProperties;
		boolean isTagLoaded = false;

		public SDHandler() {
			directory = new File(getDirectory());
			mapperDirectory = new File(getMapperDirectory());
			taggerFile = new File(getDirectory(), "tag.txt");
			taggerProperties = new Properties();			
		}

		// ... GET
		
		public String getDirectory() {
			return String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), "wifimapper");
		}

		public String getMapperDirectory() {
			return String.format("%s/%s", getDirectory(), "mapper");
		}

		public int getMapperDirectoryCount() {
			return mapperDirectory.exists() ? mapperDirectory.list().length : 0;
		}

		public String getTag(String bssid) throws Exception
		{
			this.doCreateTaggerFile();
			if (!this.isTagLoaded){
				this.taggerProperties.load(new FileInputStream(this.taggerFile));
				this.isTagLoaded = true;
			}
			return this.taggerProperties.getProperty(bssid, "");
		}
		
		// ... /GET
		
		public void createMapperFile(int session, int uniqueId, int scans, int delay, String connected,
				String deviceInfo) throws Exception {
			String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();
			this.mapperFile = new File(getMapperDirectory(), String.format("%s_%s_%s.txt", "" + session, "" + uniqueId,
					dateTime));

			long timestamp = System.currentTimeMillis() / 1000;
			doCreateMapperFile();

			String firstLine = String.format("#%d|%d|%s|%d|%d|%s|%s\n", session, uniqueId, "" + timestamp, scans,
					delay, connected, deviceInfo);
			doWriteToFile(this.mapperFile, firstLine);
		}

		// ... ADD
		
		public void addScanToFile(int scanNumber, List<ScanResult> scanResults, float[] sensors) throws Exception {
			StringBuilder string = new StringBuilder();

			long timestamp = System.currentTimeMillis() / 1000;
			string.append(String.format("$%s|%s\n", scanNumber, "" + timestamp));

			string.append(String.format("?%s\n", Arrays.toString(sensors)));

			for (ScanResult scanResult : scanResults) {
				string.append(String.format("%%%s|%s|%s|%s\n", scanResult.SSID, scanResult.BSSID, scanResult.level,
						scanResult.frequency));
			}

			doWriteToFile(this.mapperFile, string.toString());
		}

		public void addTagtoFile(String bssid, String tag) throws IOException {
			this.doCreateTaggerFile();

			String tagExisting = this.taggerProperties.getProperty(bssid);
			if (tagExisting != null && tagExisting != "") {
				tag = tagExisting + "|" + tag;
			}
			this.taggerProperties.setProperty(bssid, tag);
			this.taggerProperties.store(new FileOutputStream(this.taggerFile), null);
		}

		// ... /ADD
		
		// ... DO
		
		private void doWriteToFile(File file, String string) throws IOException {
			FileWriter fstream = new FileWriter(file, true);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(string);
			out.close();
		}
		
		private void doCreateMapperFile() throws IOException {
			if (!this.mapperFile.exists()) {
				File folder = this.mapperFile.getParentFile();
				if (!folder.exists()) {
					folder.mkdirs();
				}
				this.mapperFile.createNewFile();
			}
		}
		
		private void doCreateTaggerFile() throws IOException {
			if (!this.taggerFile.exists()) {
				File parent = this.taggerFile.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				this.taggerFile.createNewFile();
			}
		}
		
		
		// ... /DO
		
	}

	class ViewPresenter {

		private WifiMapperActivity activity;
		private TextView accessPointCountTextView;
		private TableLayout accessPointTable;
		private TextView directionTextView;
		private TextView uniqueIdTextView;
		private TextView statusTextView;
		private TextView deviceTextView;
		private Button registerButton;
		private Button stopButton;
		private EditText scansEditText;
		private EditText delayEditText;
		private EditText sessionEditText;

		public ViewPresenter(WifiMapperActivity activity) {
			this.activity = activity;

			accessPointCountTextView = (TextView) activity.findViewById(R.id.accessPointCountTextView);
			accessPointTable = (TableLayout) activity.findViewById(R.id.accessPointTable);
			directionTextView = (TextView) activity.findViewById(R.id.directionTextView);
			uniqueIdTextView = (TextView) activity.findViewById(R.id.uniqueIdTextView);
			statusTextView = (TextView) activity.findViewById(R.id.statusTextView);
			deviceTextView = (TextView) activity.findViewById(R.id.deviceTextView);
			registerButton = (Button) activity.findViewById(R.id.registerButton);
			stopButton = (Button) activity.findViewById(R.id.stopButton);
			sessionEditText = (EditText) activity.findViewById(R.id.sessionEditText);
			scansEditText = (EditText) activity.findViewById(R.id.scansEditText);
			delayEditText = (EditText) activity.findViewById(R.id.delayEditText);

			accessPointTable.setShrinkAllColumns(true);

			registerButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					handleRegister();
				}
			});
			stopButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					handleStop();
				}
			});
		}

		public void setRegisterEnabled(boolean enabled) {
			registerButton.setEnabled(enabled);
		}

		public void setStopEnabled(boolean enabled) {
			stopButton.setEnabled(enabled);
		}

		public void setSessionEnabled(boolean enabled) {
			sessionEditText.setEnabled(enabled);
		}

		public void setScansEnabled(boolean enabled) {
			scansEditText.setEnabled(enabled);
		}

		public void setDelayEnabled(boolean enabled) {
			delayEditText.setEnabled(enabled);
		}

		public int getSession() {
			try {
				return Integer.parseInt(sessionEditText.getText().toString());
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		public int getScans() {
			try {
				return Integer.parseInt(scansEditText.getText().toString());
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		public int getDelay() {
			try {
				return Integer.parseInt(delayEditText.getText().toString());
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		public void setUniqueId(String uniqueId) {
			uniqueIdTextView.setText(uniqueId);
		}

		public void setStatus(String status) {
			statusTextView.setText(status);
		}

		public void setDevice(String device) {
			deviceTextView.setText(device);
		}

		public void setSession(String session) {
			sessionEditText.setText(session);
		}

		public void setScans(String scans) {
			scansEditText.setText(scans);
		}

		public void setDelay(String delay) {
			delayEditText.setText(delay);
		}

		public void doUpdateWifiTable(Map<String, List<ScanResult>> scanResultsMap) {
			if (accessPointTable.getChildCount() > 1)
				accessPointTable.removeViews(1, accessPointTable.getChildCount() - 1);

			Set<String> keys = scanResultsMap.keySet();
			for (String key : keys) {
				doUpdateWifiTableGroup(key, scanResultsMap.get(key));
			}
		}

		private void doUpdateWifiTableGroup(String key, List<ScanResult> scanResults) {
			if (scanResults == null || scanResults.isEmpty())
				return;

			// Add header
			TableRow tableRow = new TableRow(this.activity);
			tableRow.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

			TextView textView = new TextView(this.activity);
			textView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			textView.setText(key);
			textView.setTypeface(null, Typeface.BOLD);

			tableRow.addView(textView);

			textView = new TextView(this.activity);
			textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			textView.setText("" + scanResults.size());
			textView.setGravity(Gravity.RIGHT);
			textView.setTypeface(null, Typeface.BOLD);

			tableRow.addView(textView);

			this.accessPointTable.addView(tableRow, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT));

			// List
			for (ScanResult scanResult : scanResults) {
				doUpdateWifiTableRow(scanResult);
			}
		}

		private void doUpdateWifiTableRow(ScanResult scanResult) {
			TableRow tableRow = new TableRow(this.activity);
			tableRow.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

			// BSSID
			TextView textView = new TextView(this.activity);
			textView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			textView.setText(scanResult.BSSID);
			tableRow.addView(textView);

			// Level
			textView = new TextView(this.activity);
			textView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			textView.setText("" + scanResult.level);
			textView.setGravity(Gravity.RIGHT);
			tableRow.addView(textView);

			// Frequency
			textView = new TextView(this.activity);
			textView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			textView.setText("" + scanResult.frequency);
			textView.setGravity(Gravity.RIGHT);
			tableRow.addView(textView);

			this.accessPointTable.addView(tableRow, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT));
		}

		public void doUpdateWifiCount(int size) {
			accessPointCountTextView.setText("" + size);
		}

		public void doUpdateDirection(float direction) {
			directionTextView.setText("" + direction);
		}

	}

}