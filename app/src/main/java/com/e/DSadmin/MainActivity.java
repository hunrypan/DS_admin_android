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
import android.media.AudioManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gpstime = 0;

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(null);

        startgps();

        //mapView.getMap().setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，
        //mapView.getMap().getUiSettings().setMyLocationButtonEnabled(true);


        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS)
                {
                    int result = textToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Log.d("aha","lang not supported");
                    }
                }else {
                    Log.d("aha","texttospeech init failed");
                }
            }
        });

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(getApplication(), "tcp://94.191.14.111:2000", clientId);

        try {
            client.connect().setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }



        tv = (TextView) findViewById(R.id.tv);
        thepw = (EditText) findViewById(R.id.pw);
        thessid = (EditText) findViewById(R.id.ssid);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            Log.d("aha", "ble admin not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 101);
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d("aha", "access coarse location not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
        }else if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d("aha", "access coarse location not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 103);
        }

        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
        scanLeDevice();
    }

    private MapView mapView;

    public MqttAndroidClient client;

    public IMqttMessageListener iMqttMessageListener;

    public IMqttToken iMqttToken;

    public TextToSpeech textToSpeech;

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
    private  String DS_id;
    public AMapLocationClient mLocationClient = null;
    public AMapLocationListener mLocationListener = null;
    public AMapLocationClientOption mLocationOption = null;
    public Double la;
    public Double ln;
    private  Marker marker;
    private  int gpstime;

    final static ExecutorService tpe = Executors.newSingleThreadExecutor();

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

                    //String.format("value = %d", intVar)
            Log.d("aha", "onCharacteristicRead:" + characteristic.getStringValue(0).toString());


            if (characteristic.getUuid().toString().equals(wifistate_UUID)) {
                final String wifistate = characteristic.getStringValue(0);
                bluetoothGatt.readCharacteristic(character_ssid);
                runOnUiThread(new Runnable() {
                    public void run() {
                        tv.setText("WIFI STATE: " + wifistate);
                    }
                });
            }

            if (characteristic.getUuid().toString().equals(ssid_UUID)) {
                final String ssidstr = characteristic.getStringValue(0);
                bluetoothGatt.readCharacteristic(character_pw);
                runOnUiThread(new Runnable() {
                    public void run() {
                        thessid.setText(ssidstr);
                    }
                });
            }

            if (characteristic.getUuid().toString().equals(pw_UUID)) {
                final String pwstr = characteristic.getStringValue(0);
                runOnUiThread(new Runnable() {
                    public void run() {
                        thepw.setText(pwstr);
                    }
                });
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);


            if (characteristic.getUuid().toString().equals(ssid_UUID)) {

                if (character_pw != null)
                {
                    character_pw.setValue(thepw.getText().toString());
                    bluetoothGatt.writeCharacteristic(character_pw);
                }

            }

            if (characteristic.getUuid().toString().equals(pw_UUID)) {
                if (character_wifistate != null)
                {
                    character_wifistate.setValue("open");
                    bluetoothGatt.writeCharacteristic(character_wifistate);
                }
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
            Log.d("aha", "wifistate character is " + character_wifistate.toString() );

            character_ssid = sv.getCharacteristic(UUID.fromString(ssid_UUID));
            Log.d("aha", "ssid character is " + character_ssid.toString() );


            character_pw = sv.getCharacteristic(UUID.fromString(pw_UUID));
            Log.d("aha", "pw character is " + character_pw.toString() );

            bluetoothGatt.readCharacteristic(character_wifistate);
            //bluetoothGatt.readCharacteristic(character_ssid);
            //bluetoothGatt.readCharacteristic(character_pw);

            bluetoothGatt.setCharacteristicNotification(character_wifistate, true);

            List<BluetoothGattDescriptor> des = character_wifistate.getDescriptors();

            BluetoothGattDescriptor descriptor = des.get(0);

            bluetoothGatt.readDescriptor(descriptor);

        /*
            tpe.submit(new Runnable() {
                @Override
                public void run() {
                    bluetoothGatt.readCharacteristic(character_wifistate);
                }
            });
        */

        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.d("aha","wifistate change");
            if (characteristic.getUuid().toString().equals(wifistate_UUID)) {
                final String thespeed = characteristic.getStringValue(0);

                speakhello("wifi " + thespeed);

                runOnUiThread(new Runnable() {
                    public void run() {
                        tv.setText("WIFI STATE: " + thespeed);
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
                DS_id = esp32_ble.getName();
                Log.d("aha", "esp32_ble is " + DS_id);
                speakhello("Find " + DS_id);
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

            case 102: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("aha", "onRequestPermissionsResult: " + "get ble adnim permission");
                } else {
                    Log.d("aha", "onRequestPermissionsResult: " + " not get ble adnim permission");
                }
                return;
            }

            case 103: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("aha", "onRequestPermissionsResult: " + "get ACCESS_FINE_LOCATION permission");
                } else {
                    Log.d("aha", "onRequestPermissionsResult: " + " not ACCESS_FINE_LOCATION permission");
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


    public void wifiopen(View view) {


        if (character_ssid != null)
        {
            character_ssid.setValue(thessid.getText().toString());
            bluetoothGatt.writeCharacteristic(character_ssid);
        }

    }

    public void mqttpub(String s) throws MqttException {
        MqttMessage message = new MqttMessage(s.getBytes());
        if (client.isConnected()) {
            client.publish("DrankStation_setwater", message);
        }else {
            client.connect();
            client.publish("DrankStation_setwater", message);
        }
    }

    public void speakhello(String str) {
        Bundle bundle = new Bundle();
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        textToSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, bundle, null);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    public void sendinfo(View view) {

        EditText et = (EditText)findViewById(R.id.info);

        String info = et.getText().toString();
        String sendstr = "{\"DS_id\":\"" + DS_id + "\",\"la\":\"" + String.valueOf(la) + "\",\"ln\":\"" + String.valueOf(ln) + "\",\"info\":\"" + info + "\"}";
        Log.d("aha", "sendinfo: " + sendstr);

        /*
        try {
            mqttpub(sendstr);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        */

    }


    public void  getmapinfo()
    {
        //mapView.getMap().getProjection().fromScreenLocation(android.graphics.Point paramPoint)
    }


    public  void startgps()
    {
        mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                la = aMapLocation.getLatitude();
                ln = aMapLocation.getLongitude();


                    if (la > 0) {
                            CameraUpdate mCameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(la, ln), 18, 0, 0));
                            mapView.getMap().animateCamera(mCameraUpdate);

                            LatLng latLng = new LatLng(la, ln);
                            marker = mapView.getMap().addMarker(new MarkerOptions().position(latLng).title("Drink Station").snippet(""));
                            marker.setDraggable(true);

                            AMap.OnMarkerDragListener markerDragListener = new AMap.OnMarkerDragListener() {

                                @Override
                                public void onMarkerDragStart(Marker arg0) {
                                    // TODO Auto-generated method stub

                                }

                                @Override
                                public void onMarkerDragEnd(Marker arg0) {
                                    Log.d("aha", "onMarkerDragEnd: " + arg0.getPosition().toString());

                                }

                                @Override
                                public void onMarkerDrag(Marker arg0) {
                                    // TODO Auto-generated method stub

                                }
                            };

                            mapView.getMap().setOnMarkerDragListener(markerDragListener);
                            mLocationClient.stopLocation();
                        }
            }
        };
        mLocationClient = new AMapLocationClient(getApplicationContext());

        mLocationClient.setLocationListener(mLocationListener);
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setOnceLocationLatest(true);
        mLocationOption.setNeedAddress(false);
        mLocationOption.setHttpTimeOut(10000);
        mLocationOption.setLocationCacheEnable(false);
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.startLocation();
    }

}
