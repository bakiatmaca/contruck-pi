/*
 * ConTruck-Pi
 * © 2015 Baki Turan Atmaca
 * http://bakiatmaca.com 
 * bakituranatmaca@gmail.com
 */

package com.lxport.contruckpi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.UUID;

import dfix.android.bluetooth.AsyncBluetoothClient;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener, OnTouchListener, SensorEventListener {

	private static final String TAG = "MainActivity";

	private static final UUID SPP_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //service UUID
	private static final String NODE_NAME = "rasp-lx-";

	private BluetoothAdapter _bluetoothadapter;
	protected AsyncBluetoothClient _bluetoothservice;
	protected DataChangeEvent m_stringChangeEventbluetooth;
	protected ConnectionChangeEvent m_connectionChangeEventtbluetooth;
	private SingBroadcastBluetoothReceiver _singBroadcastBluetoothReceiver;

	private boolean _isconnecting = false;

	private SensorManager mSensorManager;
	private Sensor mSensor;

	private final int CALIBRATION = 4;
	private boolean mChangedLeft = false;
	private boolean mChangedRight = false;

	private Button btn_connect;
	private Button btn_forward;
	private Button btn_back;
	private Button btn_left;
	private Button btn_right;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		btn_connect = (Button) findViewById(R.id.btn_connect);
		btn_connect.setOnClickListener(this);

		btn_forward = (Button) findViewById(R.id.btn_forward);
		btn_forward.setOnTouchListener(this);

		btn_back = (Button) findViewById(R.id.btn_back);
		btn_back.setOnTouchListener(this);

		
		btn_left = (Button) findViewById(R.id.btn_left);
		btn_left.setOnTouchListener(this);

		btn_right = (Button) findViewById(R.id.btn_right);
		btn_right.setOnTouchListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_connect:
			configuredBluetooth();
			getNodeFromBluetooth();
			_isconnecting = false;
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case R.id.btn_forward:
			if (event.getAction() == MotionEvent.ACTION_UP)
				sendRawData("forward-clr");
			else if (event.getAction() == MotionEvent.ACTION_DOWN)
				sendRawData("forward");
			return true;

		case R.id.btn_back:
			if (event.getAction() == MotionEvent.ACTION_UP)
				sendRawData("back-clr");
			else if (event.getAction() == MotionEvent.ACTION_DOWN)
				sendRawData("back");
			return true;

		case R.id.btn_right:
			if (event.getAction() == MotionEvent.ACTION_UP)
				sendRawData("right-clr");
			else if (event.getAction() == MotionEvent.ACTION_DOWN)
				sendRawData("right");
			return true;

		case R.id.btn_left:
			if (event.getAction() == MotionEvent.ACTION_UP)
				sendRawData("left-clr");
			else if (event.getAction() == MotionEvent.ACTION_DOWN)
				sendRawData("left");
			return true;

		default:
			break;

		}
		return false;
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		float y = event.values[1];

		if (y <= CALIBRATION * -1) {
			if (!mChangedLeft ) {
				sendRawData("left");
				mChangedLeft = true;
			}
		}
		else if (mChangedLeft) {
			sendRawData("left-clr");
			mChangedLeft = false;
		}

		if (y >= CALIBRATION) {
			if (!mChangedRight) {
				sendRawData("right");
				mChangedRight = true;
			}
		}
		else if (mChangedRight) {
			sendRawData("right-clr");
			mChangedRight = false;
		}
	}

	public boolean configuredBluetooth() {
		//register local BT adapter
		_bluetoothadapter = BluetoothAdapter.getDefaultAdapter();

		//check to see if there is BT on the Android device at all
		if (_bluetoothadapter == null){
			Log.d("configuredBluetooth","No Bluetooth on this handset");
			return false;
		}

		//let's make the user enable BT if it isn't already
		if (!_bluetoothadapter.isEnabled()){
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBT, 0xDEADBEEF);
		}

		//cancel any prior BT device discovery
		if (_bluetoothadapter.isDiscovering()){
			Log.d(TAG,"cancelDiscovery");
			_bluetoothadapter.cancelDiscovery();
		}

		//re-start discovery
		boolean result = _bluetoothadapter.startDiscovery();

		if (result)
			Log.d(TAG,"startDiscovery ok");
		else
			Log.e(TAG,"startDiscovery failed");

		return result;
	}

	public void getNodeFromBluetooth() {
		setInfo("Waitting for connection to " + NODE_NAME);
		_singBroadcastBluetoothReceiver = new SingBroadcastBluetoothReceiver();
		
		IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(_singBroadcastBluetoothReceiver, ifilter);

		Log.d("getNodeFromBluetooth","BroadcastBluetoothReceiver start");
	}

	private class SingBroadcastBluetoothReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {

				setInfo(NODE_NAME + " is connecting");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (device != null) {
					//if (device.getAddress().equals("00:15:83:3D:0A:57")) {
					if (device.getName().contains(NODE_NAME)) {
						if (!_isconnecting) {
							Log.d(TAG, "Device " +device.getName() + " - " + device.getAddress());

							boolean result = connectViaBluetooth(device);
							if (result) {
								Log.d(TAG, "connectViaBluetoothTo OK");
							}
							else 
								Log.d(TAG, "connectViaBluetoothTo FAILED");
						}
						else
							Log.d(TAG, "in Connecting " + device.getName());
					}
					else
						Log.d(TAG, "not match Device " +device.getName() + " - " + device.getAddress());
				} else
					Log.d(TAG, "Device is null");
			}
		}
	}

	public boolean connectViaBluetooth(BluetoothDevice device) {
		boolean result = false;

		try {
			_bluetoothservice = new AsyncBluetoothClient(_bluetoothadapter, device, SPP_UUID);
			m_stringChangeEventbluetooth = new DataChangeEvent();
			_bluetoothservice.addMessageNotifyChangeListener(m_stringChangeEventbluetooth);

			m_connectionChangeEventtbluetooth = new ConnectionChangeEvent();
			_bluetoothservice.addConnectionNotifyChangeListener(m_connectionChangeEventtbluetooth);

			_bluetoothservice.setPriority(Thread.MAX_PRIORITY);
			_bluetoothservice.start();

			result = true;
		} catch (Exception e) {
			Log.d(TAG, "connectViaBluetoothTo " + device.getName() + " - " + device.getAddress());
		}

		return result;
	}

	public class ConnectionChangeEvent implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent e) {

			int data = Integer.valueOf(e.getNewValue().toString());

			try
			{
				if (data == AsyncBluetoothClient.CLOSEDFORCELY)
				{
					_isconnecting = false;
					setEnabledConnectionBtn(!_isconnecting );
					setInfo(NODE_NAME + " connection was closed forcely");
					Log.d(TAG, "client forcely closed");
				} 
				else if (data == AsyncBluetoothClient.CLOSED)
				{
					_isconnecting = false;
					setEnabledConnectionBtn(!_isconnecting );
					setInfo(NODE_NAME + " connection was closed");
					Log.d(TAG,"closed");
				}
				else if (data == AsyncBluetoothClient.CONNECTED)
				{
					_isconnecting = true;
					setEnabledConnectionBtn(!_isconnecting );
					setInfo(NODE_NAME + " is connected");
					Log.d(TAG,"connected");
				}

			}catch(Exception ex)
			{
				Log.e(TAG, "ConnectionChangeEvent Error - ", ex);
			}
		}
	}

	public class DataChangeEvent implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent e) {
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "onAccuracyChanged");
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	private void sendRawData(String data) {
		if ((_bluetoothservice == null) || (!_bluetoothservice.isConnected()))
			return;

		_bluetoothservice.sendRawData(data);

		Log.d(TAG, "senddata:" + data);
	}

	private void setEnabledConnectionBtn(final boolean enabled) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				btn_connect.setEnabled(enabled);
			}
		});
	}

	private void setInfo(final String message) {
		final TextView lb_result = (TextView) findViewById(R.id.lb_result);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				lb_result.setText(message);
			}
		});
	}
}
