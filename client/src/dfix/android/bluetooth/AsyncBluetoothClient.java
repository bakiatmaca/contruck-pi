/*
 * AsyncBluetoothClient
 * © 2013 Baki Turan Atmaca 
 */

package dfix.android.bluetooth;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class AsyncBluetoothClient extends Thread {

	private static final String TAG = "AsyncBluetoothClient";
	
	public static final int CLOSEDFORCELY = -1;
	public static final int CLOSED = 0;
	public static final int CONNECTED = 1;
	
	protected BluetoothSocket _socket;
	protected BluetoothDevice _device;

	protected InputStream _instream = null;
	protected OutputStream _outstream = null;

	protected UUID _uuid;
	protected BluetoothAdapter _bluetoothadapter;
	protected String _deviceinfo;
	private boolean _isConnected;

	protected PropertyChangeSupport _changerecivedataevent = new PropertyChangeSupport(this);
	protected PropertyChangeSupport _changeconnectionevent = new PropertyChangeSupport(this);
	
	protected String _dataEndTag = "\n";
	protected String _encoding = "UTF-8";
	
	protected byte[] _buffer = new byte[2048];
	protected int _buffersize;
	protected String _recivedata;

	public AsyncBluetoothClient(BluetoothAdapter bluetoothadapter, BluetoothDevice device, UUID uuid) {

		_isConnected = false;
		_bluetoothadapter = bluetoothadapter;
		_device = device;
		_uuid = uuid;  

		_deviceinfo = _device.getName() + " - " + _device.getAddress(); 

		try {

			_socket = _device.createRfcommSocketToServiceRecord(_uuid);

			Log.d(TAG,"created socket for " + _uuid);

		} catch (Exception e) {
			Log.e(TAG,"cannot created socket for " + _uuid, e);
		}
	}

	@Override
	public void run() {
		if (_bluetoothadapter.isDiscovering())
			_bluetoothadapter.cancelDiscovery();

		try {
			_socket.connect();
			_instream = _socket.getInputStream();
			_outstream = _socket.getOutputStream();
			_isConnected = true;

			if (_changeconnectionevent != null)
				_changeconnectionevent.firePropertyChange("connectionNotify", null, CONNECTED);

			Log.d(TAG,"connected " + _deviceinfo);

			while (_isConnected) {

				_recivedata = null;

				_buffersize = _instream.read(_buffer);

				_recivedata = new String(_buffer, 0, _buffersize);

				if ((_recivedata != null) && (_changerecivedataevent != null)) {
					_changerecivedataevent.firePropertyChange("messageNotify", null, _recivedata);
					//Log.d("AsyncBluetoothClient [ReciveData]", _recivedata);
				}
			}
		} catch (Exception e) {

			Log.e(TAG,"connection Error",e);

			try {
				close(_isConnected);
			} catch (Exception closeException) { }
		}

	}

	public void sendData(String data)
	{
		try {
			_outstream.write((data + _dataEndTag).getBytes());
			_outstream.flush();

			//Log.d(TAG,"sendData:" + data);

		} catch (Exception e)	{ 
			Log.e(TAG,"sendData Error",e);
		}
	}
	
	public void sendRawData(String data)
	{
		try {
			_outstream.write(data.getBytes());
			_outstream.flush();

		} catch (Exception e)	{ 
			Log.e(TAG,"sendData Error",e);
		}
	}

	public void close(boolean forcely) {
		try {

			if (_isConnected)
			{
				_isConnected = false;
				_socket.close();
				
				Log.d(TAG, "Connection closed");
			}

			Log.d(TAG, "Connection parameters reseted");

		} catch (Exception e) {
			Log.e(TAG, "Connection closing Error", e);
		}

		if (_changeconnectionevent != null)
			_changeconnectionevent.firePropertyChange("connectionNotify", null, forcely ? CLOSEDFORCELY : CLOSED);

	}

	public boolean isConnected() {
		return _isConnected;
	}

	public void addMessageNotifyChangeListener(PropertyChangeListener listener)
	{
		_changerecivedataevent.addPropertyChangeListener(listener);
	}

	public void removeMessageNotifyChangeListener(PropertyChangeListener listener)
	{
		_changerecivedataevent.removePropertyChangeListener(listener);
	}

	public void addConnectionNotifyChangeListener(PropertyChangeListener listener)
	{
		_changeconnectionevent.addPropertyChangeListener(listener);
	}

	public void removeConnectionNotifyChangeListener(PropertyChangeListener listener)
	{
		_changeconnectionevent.removePropertyChangeListener(listener);
	}
}
