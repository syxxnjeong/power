package com.example.jongsal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main2Activity extends AppCompatActivity {
    TextView mTvBluetoothStatus;
    Button mBtnBluetoothOn;
    Button mBtnBluetoothOff;
    Button mBtnConnect;
    Button mBtnStartResourceActivity;

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3; // 상수 정의
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mTvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        mBtnBluetoothOn = findViewById(R.id.btnBluetoothOn);
        mBtnBluetoothOff = findViewById(R.id.btnBluetoothOff);
        mBtnConnect = findViewById(R.id.btnConnect);
        mBtnStartResourceActivity = findViewById(R.id.btnStartResourceActivity);

        // BluetoothAdapter 초기화 시 BluetoothManager 사용
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBtnBluetoothOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOn();
            }
        });

        mBtnBluetoothOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOff();
            }
        });

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });

        mBtnStartResourceActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main2Activity.this, resourceActivity.class);
                startActivity(intent);
            }
        });

        mBluetoothHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == BT_MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // 데이터 수신 후 처리
                    Log.d("Main2Activity", "Received data: " + readMessage); // 수신된 데이터 로그

                    // 데이터를 resourceActivity로 전송
                    Intent intent = new Intent(BluetoothService.ACTION_DATA_AVAILABLE);
                    intent.putExtra(BluetoothService.EXTRA_DATA, readMessage);
                    sendBroadcast(intent);
                } else if (msg.what == BT_CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        mTvBluetoothStatus.setText("활성화 상태입니다");
                    } else {
                        mTvBluetoothStatus.setText("비활성화 상태입니다");
                    }
                }
            }
        };
    }

    void bluetoothOn() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth already enabled", Toast.LENGTH_LONG).show();
            }
        }
    }

    void bluetoothOff() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "Bluetooth disabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth already disabled", Toast.LENGTH_SHORT).show();
        }
    }

    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();
            mListPairedDevices = new ArrayList<>();
            for (BluetoothDevice device : mPairedDevices) {
                mListPairedDevices.add(device.getName());
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select device");
            builder.setItems(mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    connectSelectedDevice(mListPairedDevices.get(i));
                }
            });
            builder.create().show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
        }
    }

    void connectSelectedDevice(String selectedDeviceName) {
        for (BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        if (mBluetoothDevice != null) {
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                mBluetoothSocket.connect();
                mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
                mThreadConnectedBluetooth.start();

                mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Bluetooth connection failed", Toast.LENGTH_LONG).show();
                Log.e("MainActivity", "Bluetooth connection failed", e); // 로그 추가
            }
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0) {
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
