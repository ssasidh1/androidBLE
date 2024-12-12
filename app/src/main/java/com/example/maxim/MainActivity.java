package com.example.maxim;

import static com.example.maxim.R.*;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.*;
public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler;
    private boolean scanning;
    private ScanSettings scanSettings;
    private ArrayList<String> devicesList;
    private ArrayAdapter<String> deviceAdapter;
    private static final long SCAN_PERIOD = 10000;
    ArrayList<HashMap<String, String>> characteristicsList;
    Button scanBt;
    ListView listView;
    private BluetoothLeService bluetoothLeService;
    private boolean connected = false;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private  static final String targetServiceUUID = "00001523-1212-EFDE-1523-785FEABCD123";
    private  static final String targetCharacteristicUUID = "00001527-1212-efde-1523-785feabcd123";
    private final HashMap<String, String> uniqueCharacteristics = new HashMap<>();
    private void handleDiscoveredServices() {
        List<BluetoothGattService> services = bluetoothLeService.getSupportedGattServices();
        if (services == null) return;
        for (BluetoothGattService service : services) {
            // Log.w("CCValue", "Inside handleDiscoverservices"+service.getUuid().toString());
            if (service.getUuid().toString().equals(targetServiceUUID.toLowerCase())) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    HashMap<String, String> characteristicData = new HashMap<>();
                    characteristicData.put("UUID", characteristic.getUuid().toString());
                    characteristicData.put("Data", "N/A"); // Optional: Include properties
                    characteristicsList.add(characteristicData);
                }
            }
        }
        Intent intent = new Intent(this, CharacteristicsActivity.class);
        intent.putExtra("CHARACTERISTICS_LIST", characteristicsList);
        startActivity(intent);

    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("CCValue", "Inside OnReceive" + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                updateConnectionState("Connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                updateConnectionState("Disconnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                handleDiscoveredServices();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                String dataType = intent.getStringExtra("DATA_TYPE");

                // Handle data based on type
                if ("BPM".equals(dataType)) {
                    int bpm = intent.getIntExtra("DATA_SINGLE", 0);
                    handleBPMData(uuid, bpm);
                }else if ("TEMP".equals(dataType)) {
                    int temp = intent.getIntExtra("DATA_SINGLE", 0);
                     handleTEMPData(uuid, temp);
                }else if ("ECG".equals(dataType)) {
                    Log.w("ecgMain","data = ");
                    short[] ecgData = intent.getShortArrayExtra("DATA_ARRAY_BOXED");
                    Log.w("ecgMain","data = "+ecgData);
                    handleECGData(uuid, ecgData);
                } else if ("HRV".equals(dataType)) {
                    byte[] hrvData = intent.getByteArrayExtra("DATA_ARRAY_BYTE");
                    handleHRVData(uuid, hrvData);
                } else if ("QRS".equals(dataType)) {
                    short[] qrsData = intent.getShortArrayExtra("DATA_ARRAY_BOXED");
                    handleQRSData(uuid, qrsData);
                }
            }
        }
    };



        private void handleBPMData(String uuid, int bpm) {
            Intent intent = new Intent(MainActivity.this, CharacteristicsActivity.class);
            intent.putExtra("UUID", uuid);
            intent.putExtra("DATA_TYPE", "BPM");
            intent.putExtra("DATA_SINGLE", bpm);
            sendBroadcast(intent);
        }
    private void handleTEMPData(String uuid, int temp) {
        Intent intent = new Intent(MainActivity.this, CharacteristicsActivity.class);
        intent.putExtra("UUID", uuid);
        intent.putExtra("DATA_TYPE", "TEMP");
        intent.putExtra("DATA_SINGLE", temp);
        sendBroadcast(intent);
    }

        private void handleECGData(String uuid, short[] ecgData) {
            Intent intent = new Intent(MainActivity.this, CharacteristicsActivity.class);
            runOnUiThread(()->{
                intent.putExtra("UUID", uuid);
                intent.putExtra("DATA_TYPE", "ECG");
                intent.putExtra("DATA_ARRAY_BOXED", ecgData);
                sendBroadcast(intent);
            });
        }

        private void handleHRVData(String uuid, byte[] hrvData) {
            Intent intent = new Intent(MainActivity.this, CharacteristicsActivity.class);
            intent.putExtra("UUID", uuid);
            intent.putExtra("DATA_TYPE", "HRV");
            intent.putExtra("DATA_ARRAY_BYTE", hrvData);
            sendBroadcast(intent);
        }

        private void handleQRSData(String uuid, short[] qrsData) {
            Intent intent = new Intent(MainActivity.this, CharacteristicsActivity.class);
            intent.putExtra("UUID", uuid);
            intent.putExtra("DATA_TYPE", "QRS");
            intent.putExtra("DATA_ARRAY_BOXED", qrsData);
            sendBroadcast(intent);
        }




    private void displayTemperature(String temp) {
        // Update the UI or ListView with the temperature
        Log.d( "CCValue " ,"temperature"+ temp + "°");
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
        PermissionsUtility.checkPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT);
        // high-level api for system services, oversees the overall operation of a system or service
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // hardware-specific and provides direct access to the component it represents. It is a singleton.
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        }
        handler = new Handler();

        listView = findViewById(id.dev_list_view);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        devicesList = new ArrayList<>();
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, devicesList);
        listView.setAdapter(deviceAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = devicesList.get(position);
            String deviceAddress = deviceInfo.substring(deviceInfo.indexOf("(") + 1, deviceInfo.indexOf(")"));
            //Toast.makeText(MainActivity.this, "OnClick.", Toast.LENGTH_SHORT).show();
            if (bluetoothLeService != null) {
                bluetoothLeService.connect(deviceAddress);
                //Toast.makeText(MainActivity.this, "Service ready.", Toast.LENGTH_SHORT).show();
                Log.d("BLEGATT", "Attempting to connect to: " + deviceAddress);
            } else {
                //Toast.makeText(MainActivity.this, "Service not ready. Try again.", Toast.LENGTH_SHORT).show();
                Log.d("BLEGATT", "BluetoothLeService is not initialized.");
            }
        });
        scanBt = findViewById(id.scan_Btn);
        scanBt.setOnClickListener(v -> {
            scanLeDevice();
        });

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            bluetoothLeService = binder.getService();
            bluetoothLeService.setBluetoothAdapter(bluetoothAdapter);
            listView.setEnabled(true);
            Log.d("BLESERV", "BluetoothLeService connected.");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            Log.d("BLESERV", "BluetoothLeService disconnected.");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("BLESERV", "Binding BluetoothLeService.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        bluetoothLeService = null;
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    private String readCharacteristicValue(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();

        if (value != null) {
            return new String(value); // Convert byte array to string
        }
        return "N/A";
    }

    private void displayData(String data) {
        Log.d("BLEData", "Received data: " + data);
        // Update the UI with the data
        Toast.makeText(this, "Data: " + data, Toast.LENGTH_SHORT).show();
    }

    private void updateConnectionState(final String state) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Connection State: " + state, Toast.LENGTH_SHORT).show();
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scanLeDevice() {
        if (!PermissionsUtility.checkPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {
            PermissionsUtility.requestPermissions(MainActivity.this,android.Manifest.permission.BLUETOOTH_CONNECT,MainActivity.this);
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "Bluetooth LE Scanner is not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("BLE_SCAN", "Showing toast: Starting BLE Scan...");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!scanning) {
            Toast.makeText(this, "Starting BLE Scan....", Toast.LENGTH_SHORT).show();
            scanning = true;
            scanBt.setText("Stop Scan");
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            scanBt.setText("Start Scan");
            devicesList.clear();
            deviceAdapter.notifyDataSetChanged();
            bluetoothLeScanner.stopScan(leScanCallback);
            Toast.makeText(this, "Scanning Stopped ", Toast.LENGTH_SHORT).show();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (!PermissionsUtility.checkPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {
                PermissionsUtility.requestPermissions(MainActivity.this,android.Manifest.permission.BLUETOOTH_CONNECT,MainActivity.this);
                return;
            }
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            String deviceAddress = device.getAddress();

            if (deviceName == null) deviceName = "Unknown Device";

            String deviceInfo = deviceName + " (" + deviceAddress + ")";
            if(!devicesList.contains(deviceInfo)){
                devicesList.add(deviceInfo);
                deviceAdapter.notifyDataSetChanged();
            }

        }
        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (!PermissionsUtility.checkPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {
                PermissionsUtility.requestPermissions(MainActivity.this,android.Manifest.permission.BLUETOOTH_CONNECT,MainActivity.this);
                return;
            }
            for (ScanResult result : results) {
                BluetoothDevice device = result.getDevice();
                String deviceName = device.getName();

                String deviceAddress = device.getAddress();

                if (deviceName == null) deviceName = "Unknown Device";

                String deviceInfo = deviceName + " (" + deviceAddress + ")";
                if (!devicesList.contains(deviceInfo)) {
                    devicesList.add(deviceInfo);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this, "Scan failed with error: " + errorCode, Toast.LENGTH_SHORT).show();
        }

    };


    @RequiresApi(Build.VERSION_CODES.S)
    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT},
                PERMISSION_REQUEST_CODE);
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }


}

























