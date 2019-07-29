package com.e.DSadmin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.tv);
        thepw = (EditText) findViewById(R.id.pw);
        thessid = (EditText) findViewById(R.id.ssid);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            Log.d("aha", "ble admin not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 101);
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d("aha", "access coarse location not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }

        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
        scanLeDevice();
    }

    private HttpURLConnection urlConnection;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothDevice esp32_ble;

    private BluetoothLeScanner scanner;

    private BluetoothGatt bluetoothGatt;

    private boolean mScanning;

    boolean finded = false;

    public BluetoothGattService sv;

    public BluetoothGattCharacteristic character_wifistate;
    public BluetoothGattCharacteristic  character_ssid;
    public BluetoothGattCharacteristic character_pw;

    private final static String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private final static String wifistate_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    private final static String ssid_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a1";
    private final static String pw_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a2";
    public TextView tv;
    public EditText thessid;
    public EditText thepw;

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            //String.format("value = %d", intVar)

            if (characteristic.getUuid().toString().equals(wifistate_UUID)) {
                Log.d("aha", "onCharacteristicRead:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32,0).toString());
                final String wifistate = characteristic.getStringValue(0);
                runOnUiThread(new Runnable() {
                    public void run() {
                        tv.setText("WIFI STATE " + wifistate);
                    }
                });
            } else if (characteristic.getUuid().toString().equals(ssid_UUID)) {
                final String ssidstr = characteristic.getStringValue(0);
                runOnUiThread(new Runnable() {
                    public void run() {
                        thessid.setText(ssidstr);
                    }
                });
            }else if (characteristic.getUuid().toString().equals(pw_UUID)) {
                final String pwstr = characteristic.getStringValue(0);
                runOnUiThread(new Runnable() {
                    public void run() {
                        thepw.setText(pwstr);
                    }
                });
            }
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            Log.d("aha", "onDescriptorRead: " + descriptor.toString());

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            if (bluetoothGatt.writeDescriptor(descriptor))
            {
                Log.d("aha", "write descriptor ok ");
            }else {
                Log.d("aha", "write descriptor false");
            }

        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("aha", "onDescriptorWrite: ");
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d("aha", "onServicesDiscovered: sv finded");
            sv = bluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
            Log.d("aha", "onServicesDiscovered: " + sv.getUuid().toString());
            sv.getCharacteristics();

            character_wifistate = sv.getCharacteristic(UUID.fromString(wifistate_UUID));

            character_ssid = sv.getCharacteristic(UUID.fromString(ssid_UUID));
            character_pw = sv.getCharacteristic(UUID.fromString(pw_UUID));

            bluetoothGatt.readCharacteristic(character_wifistate);

            bluetoothGatt.readCharacteristic(character_ssid);

            bluetoothGatt.readCharacteristic(character_pw);

            /*
            bluetoothGatt.setCharacteristicNotification(character_windspeed, true);

            // bluetoothGatt.readCharacteristic(character_windspeed);

            List<BluetoothGattDescriptor> des = character_windspeed.getDescriptors();
            Log.d("aha", "how many des : " + des.size());


            BluetoothGattDescriptor descriptor = des.get(0);


            Log.d("aha", "descriptor is: " + descriptor.toString());

            bluetoothGatt.readDescriptor(descriptor);
            */
            //character_looptime = sv.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID2));
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.d("aha","windspeed change");
            if (characteristic.getUuid().toString().equals(wifistate_UUID)) {
                final String thespeed = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0).toString();
                Log.d("aha", "windspeed change to: " + thespeed);
                runOnUiThread(new Runnable() {
                    public void run() {
                        tv.setText("windspeed: " + thespeed);
                    }
                });
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("aha", "onConnectionStateChange: " + newState);
            if (newState == 2) {
                bluetoothGatt.discoverServices();
            }else if (newState == 0)
            {
                Log.d("aha", "onConnectionStateChange: disconnection ");
                character_wifistate = null;
                bluetoothGatt.connect();
            }
        }
    };


    private ScanCallback scanCallback = new ScanCallback() {

        private ScanCallback scanCallbackend = new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (esp32_ble == null) {
                esp32_ble = result.getDevice();
                bluetoothGatt = esp32_ble.connectGatt(getApplicationContext(), true, gattCallback);
                Log.d("aha", "esp32_ble is " + esp32_ble.getName());
                bluetoothGatt.connect();

            } else {
                if (mScanning)
                    scanner.stopScan(scanCallbackend);
                mScanning = false;
            }
        }
    };


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("aha", "onRequestPermissionsResult: " + "get ble adnim permission");
                } else {
                    Log.d("aha", "onRequestPermissionsResult: " + " not get ble adnim permission");
                }
                return;
            }
            default:
        }
    }


    private void scanLeDevice() {

        Log.d("aha", "to scanLeDevice: ");
        scanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build();

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERVICE_UUID)).build();
        filters.add(filter);
        mScanning = true;
        scanner.startScan(filters, settings, scanCallback);
    }


}
