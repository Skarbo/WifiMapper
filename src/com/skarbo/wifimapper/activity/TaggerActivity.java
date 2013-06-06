package com.skarbo.wifimapper.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.skarbo.wifimapper.R;
import com.skarbo.wifimapper.model.WifiScan;

public class TaggerActivity extends WifiListActivity {

	protected static final String TAG = "TaggerActivity";
	private static final int DIALOG_TAG = 1;
	private TaggerPresenter presenter;
	private boolean isInit;
	protected WifiScan wifiScanSelected;

	// ... ON

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!this.isInit) {
			doInit();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_TAG:
			return presenter.getAlertDialog();
		}

		return null;
	}

	// ... /ON

	// ... IS

	@Override
	protected boolean isShowTag() {
		return true;
	}

	// ... /IS

	// ... DO

	private void doInit() {
		// Initiate presenter
		this.presenter = new TaggerPresenter();

		// Handle item click
		getListView().setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				wifiScanSelected = getScanResults().get(position);

				if (wifiScanSelected != null) {
					presenter.updateDialog(wifiScanSelected);
					showDialog(DIALOG_TAG);
				}
			}
		});

		this.isInit = true;
	}

	// ... /DO

	// ... HANDLE

	private void handleTag(String tag) {
		if (this.wifiScanSelected == null) {
			return;
		}

		try {
			if (tag.equalsIgnoreCase("")) {
				this.wifiHandler.getSdHandler().removeTag(this.wifiScanSelected.getScanResult());
			} else {
				this.wifiHandler.getSdHandler().addTag(tag, this.wifiScanSelected.getScanResult());
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}

		this.wifiScanSelected.setTag(tag);
		this.getAdapter().notifyDataSetChanged();

	}

	// ... /HANDLE

	// ... CLASS

	private class TaggerPresenter {

		private AlertDialog alertDialog;
		private View dialogView;
		private EditText tagEditText;
		private TextView tagSsidTextView;
		private TextView tagBssidTextView;

		public TaggerPresenter() {
			LayoutInflater factory = LayoutInflater.from(TaggerActivity.this);

			// Get dialog view
			this.dialogView = factory.inflate(R.layout.tag_dialog, null);
			this.tagEditText = (EditText) this.dialogView.findViewById(R.id.wifi_tag_edittext);
			this.tagSsidTextView = (TextView) this.dialogView.findViewById(R.id.wifi_tag_ssid_textview);
			this.tagBssidTextView = (TextView) this.dialogView.findViewById(R.id.wifi_tag_bssid_textview);

			// Create alert dialog
			this.alertDialog = new AlertDialog.Builder(TaggerActivity.this).setIcon(R.drawable.label_new)
					.setTitle(R.string.wifi_tag).setView(this.dialogView)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							handleTag(tagEditText.getText().toString());
						}
					}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Log.d(TAG, "On click: Cancel");
						}
					}).create();

		}

		public AlertDialog getAlertDialog() {
			return alertDialog;
		}

		public void updateDialog(WifiScan scan) {
			this.tagSsidTextView.setText(scan.getScanResult().SSID);
			this.tagBssidTextView.setText(scan.getScanResult().BSSID);
			this.tagEditText.setText(scan.getTag());
		}

	}

	public void handleWifiTick(long progress, long max) {
		// TODO Auto-generated method stub

	}

	// ... /CLASS

}
