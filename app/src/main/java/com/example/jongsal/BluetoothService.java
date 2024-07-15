package com.example.jongsal;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String ACTION_DATA_AVAILABLE = "com.example.jongsal.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "com.example.jongsal.EXTRA_DATA";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private ConnectedThread mConnectedThread;
    private boolean mIsServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            stopSelf();
            return;
        }
        mIsServiceRunning = true;
        Log.d(TAG, "BluetoothService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        connectToDevice(device);
        Log.d(TAG, "onStartCommand: connecting to device " + deviceAddress);
        return START_STICKY;
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            boolean connected = false;
            int attempt = 0;
            while (!connected && attempt < 3) { // 최대 3번 재시도
                try {
                    mBluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
                    mBluetoothSocket.connect();
                    Log.d(TAG, "Bluetooth connected");
                    mConnectedThread = new ConnectedThread(mBluetoothSocket);
                    mConnectedThread.start();
                    connected = true;
                } catch (IOException e) {
                    Log.e(TAG, "Error while connecting, attempt: " + attempt, e);
                    attempt++;
                    try {
                        Thread.sleep(1000); // 재시도 전 1초 대기
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "Thread interrupted", ie);
                    }
                    try {
                        mBluetoothSocket.close();
                    } catch (IOException closeException) {
                        Log.e(TAG, "Could not close the client socket", closeException);
                    }
                }
            }
            if (!connected) {
                Log.e(TAG, "Unable to establish Bluetooth connection after retries");
                stopSelf();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsServiceRunning = false;
        Log.d(TAG, "BluetoothService destroyed");
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.d(TAG, "I/O streams created");
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating I/O streams", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (mIsServiceRunning) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0) {
                        String readMessage = new String(buffer, 0, bytes, "UTF-8");
                        Log.d(TAG, "Received data: " + readMessage);
                        broadcastUpdate(readMessage);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when reading data", e);
                    break;
                }
            }
        }

        private void broadcastUpdate(final String data) {
            final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
            intent.putExtra(EXTRA_DATA, data);
            Log.d(TAG, "Broadcasting data: " + data);
            sendBroadcast(intent);
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when closing socket", e);
            }
        }
    }
}
