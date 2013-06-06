package com.skarbo.wifimapper.activity;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.skarbo.wifimapper.R;
import com.skarbo.wifimapper.handler.WifiHandler;
import com.skarbo.wifimapper.listener.WifiListener;
import com.skarbo.wifimapper.model.WifiMap;
import com.skarbo.wifimapper.model.WifiMapper;
import com.skarbo.wifimapper.model.WifiScan;

public class MapperActivity extends Activity implements WifiListener {

	private static final String TAG = "MapperActivity";
	private static final int SESSION_DEFAULT = 1;
	private static final int SCANS_DEFAULT = 5;
	private static final int DELAY_DEFAULT = 5;
	private static final int ID_DEFAULT = 0;
	private static final float DIRECTION_DEFAULT = 0.0f;
	private static final String PREF_SESSION = "session";
	private static final String PREF_SCANS = "scans";
	private static final String PREF_DELAY = "delay";
	private static final int STATE_STARTING = 1;
	public static final int STATE_REGISTER = 2;
	public static final int STATE_FINISHED = 3;
	private static final int STATE_INIT = 4;

	private boolean isInit = false;
	private WifiHandler wifiHandler;
	private MapperPresenter presenter;
	private SharedPreferences preferences;
	private SensorManager sensorManager;
	private Sensor sensor;
	private int session = SESSION_DEFAULT;
	private int scans = SCANS_DEFAULT;
	private int delay = DELAY_DEFAULT;
	private boolean isRegister = false;
	private int uniqueId = ID_DEFAULT;
	private int registerCount;
	private WifiMapper wifiMapper;
	private float[] sensors = { 0.0f, 0.0f, 0.0f };
	private SensorListener sensorListener;

	// ... ON

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapper);

		doInit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Unlisten WiFi
		this.wifiHandler.doWifiUnlisten();

		this.isInit = false;

		// Store preferences
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(PREF_SESSION, this.session);
		editor.putInt(PREF_SCANS, this.scans);
		editor.putInt(PREF_DELAY, this.delay);
		boolean commited = editor.commit();
	}

	// ... /ON

	// ... GET

	public Context getContext() {
		return this.getApplicationContext();
	}

	// ... /GET

	// ... DO

	private void doInit() {
		if (this.isInit) {
			return;
		}

		// Preferences
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Mapper presenter
		this.presenter = new MapperPresenter();

		// WiFi handler
		this.wifiHandler = new WifiHandler(this);

		// Sensor
		this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		this.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		this.sensorListener = new SensorListener();

		// Listen to WiFi
		this.wifiHandler.doWifiListen();

		// Listen to sensor
		this.sensorManager.registerListener(this.sensorListener, this.sensor, SensorManager.SENSOR_DELAY_NORMAL);

		// Set default values
		this.session = preferences.getInt(PREF_SESSION, SESSION_DEFAULT);
		this.scans = preferences.getInt(PREF_SCANS, SCANS_DEFAULT);
		this.delay = preferences.getInt(PREF_DELAY, DELAY_DEFAULT);

		// Update presenter
		this.presenter.doUpdate(STATE_INIT);

		this.isInit = true;
	}

	private void doStartRegister() throws Exception {
		if (this.isRegister) {
			throw new Exception("Register already in progess");
		}
		if (!this.wifiHandler.isWifiEnabled()) {
			throw new Exception("Wifi is not enabled");
		}
		if (!this.wifiHandler.isWifiConnected()) {
			throw new Exception("Wifi is not connected");
		}

		this.session = presenter.getSession();
		this.scans = presenter.getScans();
		this.delay = presenter.getDelay();

		if (this.session < 1) {
			throw new Exception("Session must exceed 0");
		}
		if (this.scans < 1) {
			throw new Exception("Scans must exceed 0");
		}
		if (this.delay < 1) {
			throw new Exception("Delay must exceed 0");
		}

		// Set values
		this.uniqueId = this.wifiHandler.getNextUniqueId();
		this.isRegister = true;
		this.registerCount = 0;

		// Create device string
		String device = String.format("%s, %s, %s", android.os.Build.MODEL, android.os.Build.VERSION.RELEASE,
				android.os.Build.BOOTLOADER);

		// Create WiFi mapper
		this.wifiMapper = new WifiMapper(this.session, this.uniqueId, this.scans, this.delay, this.wifiHandler
				.getWifiManager().getConnectionInfo(), device);

		// Create WiFi mapper file
		try {
			this.wifiHandler.getSdHandler().createMapperFile(this.wifiMapper);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			this.doStopRegister();
			throw new Exception("Can't create Wi-Fi mapper file");
		}

		// Set WiFi handler delay
		this.wifiHandler.setDelay(this.delay * 1000);

		// Notify
		doNotify(String.format("Starting register session #%d, id #%d with %d scan(s) and %d sec. delay", this.session,
				this.uniqueId, this.scans, this.delay), true);

		// Update presenter
		this.presenter.doUpdate(STATE_STARTING);
	}

	private void doStopRegister() {
		doNotify(String.format("Register session #%d, id #%d stopped; %d scan(s) registered", this.session,
				this.uniqueId, this.registerCount), true);
		// Set values
		this.isRegister = false;

		// Update presenter
		this.presenter.doUpdate(STATE_FINISHED);

		// Set WiFi scan to default
		this.wifiHandler.setDelayDefault();
	}

	private void doNotify(String message) {
		doNotify(message, false);
	}

	private void doNotify(String message, boolean longMessage) {
		(Toast.makeText(this, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
	}

	// ... /DO

	// ... HANDLE

	public void handleWifiScan(List<WifiScan> wifiScans) {
		if (this.isRegister) {
			try {
				// Increase register count
				this.registerCount++;

				// Create WiFi map
				WifiMap wifiMap = new WifiMap(this.wifiMapper, this.registerCount, this.sensors, wifiScans);

				// Add WiFi map to file
				this.wifiHandler.getSdHandler().addScanToFile(wifiMap);

				// Update presenter
				this.presenter.doUpdate(STATE_REGISTER);

				// Finish register
				if (this.registerCount >= this.scans) {
					doStopRegister();
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
				doNotify("Error while adding scan to file");
				doStopRegister();
			}
		}
	}

	private void handleRegisterStart() {
		try {
			this.doStartRegister();
		} catch (Exception e) {
			doNotify(e.getMessage());
		}
	}

	private void handleRegisterStop() {
		try {
			this.doStopRegister();
		} catch (Exception e) {
			doNotify(e.getMessage());
		}
	}

	private void handleRecievedSensor(float[] values) {
		this.sensors = values;
		this.presenter.doUpdateDirection((float) values[0]);
	}

	// ... /HANDLE

	// CLASS

	private class MapperPresenter {

		private NumberButtonsHandler sessionNumberButtons;
		private NumberButtonsHandler scansNumberButtons;
		private NumberButtonsHandler delayNumberButtons;
		private Button registerButton;
		private Button stopButton;
		private TextView statusTextView;
		private ProgressBar statusProgessBar;
		private TextView idTextView;
		private TextView directionTextView;

		public MapperPresenter() {

			// Number buttons
			this.sessionNumberButtons = new NumberButtonsHandler((View) findViewById(R.id.map_session_number_buttons));
			this.scansNumberButtons = new NumberButtonsHandler((View) findViewById(R.id.map_scans_number_buttons));
			this.delayNumberButtons = new NumberButtonsHandler((View) findViewById(R.id.map_delay_number_buttons));

			this.statusTextView = (TextView) findViewById(R.id.map_status_textview);
			this.statusProgessBar = (ProgressBar) findViewById(R.id.map_status_progressbar);
			this.idTextView = (TextView) findViewById(R.id.map_id_textview);
			this.directionTextView = (TextView) findViewById(R.id.map_direction_textview);
			this.registerButton = (Button) findViewById(R.id.map_register_button);
			this.stopButton = (Button) findViewById(R.id.map_stop_button);

			this.sessionNumberButtons.updateNumber(session);
			this.scansNumberButtons.updateNumber(scans);
			this.delayNumberButtons.updateNumber(delay);
			this.directionTextView.setText("" + DIRECTION_DEFAULT);
			this.idTextView.setText("" + ID_DEFAULT);

			this.registerButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					handleRegisterStart();
				}
			});

			this.stopButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					handleRegisterStop();
				}
			});
		}

		public void doUpdateDirection(float direction) {
			this.directionTextView.setText("" + direction);
		}

		public int getSession() {
			return this.sessionNumberButtons.getNumber();
		}

		public int getScans() {
			return this.scansNumberButtons.getNumber();
		}

		public int getDelay() {
			return this.delayNumberButtons.getNumber();
		}

		public void doUpdate(int state) {
			switch (state) {
			case STATE_INIT:
				this.statusTextView.setText("Ready");
				this.sessionNumberButtons.updateNumber(session);
				this.scansNumberButtons.updateNumber(scans);
				this.delayNumberButtons.updateNumber(delay);
				this.idTextView.setText("" + uniqueId);
				break;
			case STATE_STARTING:
				setEnabled(!isRegister);
				this.statusProgessBar.setProgress(registerCount);
				this.statusProgessBar.setMax(scans);
				this.statusTextView.setText("Starting...");
				this.idTextView.setText("" + uniqueId);
				break;
			case STATE_REGISTER:
				this.statusTextView.setText(String.format("%d/%d registered", registerCount, scans));
				this.statusProgessBar.setProgress(registerCount);
				break;
			case STATE_FINISHED:
				setEnabled(!isRegister);
				this.statusTextView.setText("Ready");
				break;
			}
		}

		private void setEnabled(boolean enabled) {
			this.sessionNumberButtons.setEnable(enabled);
			this.scansNumberButtons.setEnable(enabled);
			this.delayNumberButtons.setEnable(enabled);
			this.stopButton.setEnabled(!enabled);
			this.registerButton.setEnabled(enabled);
			this.statusProgessBar.setVisibility(enabled ? View.GONE : View.VISIBLE);
		}

	}

	private class NumberButtonsHandler {

		private Button incButton;
		private Button decButton;
		private EditText editText;

		public NumberButtonsHandler(View view) {

			this.incButton = (Button) view.findViewById(R.id.number_button_increase);
			this.decButton = (Button) view.findViewById(R.id.number_button_decrease);
			this.editText = (EditText) view.findViewById(R.id.number_button_edittext);

			this.incButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					int number = getNumber();
					editText.setText("" + (number + 1));
				}
			});
			this.decButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					int number = getNumber();
					editText.setText("" + Math.max(number - 1, 1));
				}
			});
		}

		public int getNumber() {
			try {
				return Integer.parseInt(editText.getText().toString());
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		public void updateNumber(int number) {
			editText.setText("" + number);
		}

		public void setEnable(boolean enable) {
			editText.setEnabled(enable);
			incButton.setEnabled(enable);
			decButton.setEnabled(enable);
		}

	}

	private class SensorListener implements SensorEventListener {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void onSensorChanged(SensorEvent event) {
			if (event.values.length > 0) {
				handleRecievedSensor(event.values);
			}
		}

	}

	public void handleWifiTick(long progress, long max) {
		// TODO Auto-generated method stub

	}

	// /CLASS

}
