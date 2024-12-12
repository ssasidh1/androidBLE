package com.example.maxim;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    public static final String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public static final String EXTRA_UUID = "com.example.maxim.EXTRA_UUID";
    private  static final String targetServiceUUID = "00001523-1212-efde-1523-785feabcd123";
    private  static final String temperatureUUID = "00001527-1212-efde-1523-785feabcd123";
    private  static final String ecgUUID = "00001526-1212-efde-1523-785feabcd123";
    private  static final String hrUUID = "00001525-1212-efde-1523-785feabcd123";
    private  static final String hrvUUID = "00001524-1212-efde-1523-785feabcd123";
    private  static final String dcUUID = "00001529-1212-efde-1523-785feabcd123";
    private  static final String modeUUID = "0000152a-1212-efde-1523-785feabcd123";
    private  static final String qrsUUID = "00001528-1212-efde-1523-785feabcd123";


    private BluetoothGatt bluetoothGatt;
    private BluetoothManager bluetoothManager;
    private int connectionState = BluetoothGatt.STATE_DISCONNECTED;
    private BluetoothAdapter bluetoothAdapter;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder; // No binding required
    }
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
    public void setBluetoothAdapter(BluetoothAdapter adapter) {
        this.bluetoothAdapter = adapter;
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                Log.d("GATTCallback", "Connected to GATT server.");
                broadcastUpdate(ACTION_GATT_CONNECTED);
                if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    return;
                }
                bluetoothGatt.discoverServices(); // Discover services
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                Log.d("GATTCallback", "Disconnected from GATT server.");
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d("CCValue", "Inside onServiceDiscovered");
                for (BluetoothGattService service : bluetoothGatt.getServices()) {
                    Log.d("CCValue", "Inside onServiceDiscovered Service"+service.getUuid());
                    if (service.getUuid().toString().equals(targetServiceUUID.toLowerCase())) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            Log.d("CCValue", "Inside onServiceDiscovered Charac"+characteristic.getUuid());
                            if(!characteristic.getUuid().toString().equals(modeUUID) &&  !characteristic.getUuid().toString().equals(dcUUID)){
                                enableNotifications(characteristic,characteristic.getUuid()); // Enable notifications
                            }


                        }
                    }
                }

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.w("CCValue", "characteristicRead"+characteristic.getValue().toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("CCValue", "characteristicRead"+characteristic.getValue().toString());
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            Log.e("CCValue", "uuid = "+characteristic.getUuid().toString()+" valye " + (value == null ? "null" : Arrays.toString(value)));
             if (characteristic.getUuid().toString().equals(hrUUID)) {
                decodeBPM(value);
            } else if (characteristic.getUuid().toString().equals(ecgUUID)) {
                decodeECG(value);
            } else if (characteristic.getUuid().toString().equals(hrvUUID)) {
                decodeHRV(value);
            } else if (characteristic.getUuid().toString().equals(qrsUUID)) {
                decodeQRS(value);
            }
             else if (characteristic.getUuid().toString().equals(temperatureUUID)) {
                 decodeTemperature(value);
             }
             else {
                Log.e("CCValue", "Invalid data received: " + (value == null ? "null" : Arrays.toString(value)));
            }
        }
    };




    private void decodeBPM(byte[] value) {
        if (value != null && value.length == 4) {
            int bpm = ((value[0] & 0xFF) << 24) |
                    ((value[1] & 0xFF) << 16) |
                    ((value[2] & 0xFF) << 8) |
                    (value[3] & 0xFF);
            broadcastUpdate(ACTION_DATA_AVAILABLE, "BPM", bpm,hrUUID);
        }
    }
    private void decodeECG(byte[] value) {
        if (value != null && value.length % 2 == 0) {

            int n = value.length / 2;
            short[] ecgData = new short[n];
            for (int i = 0; i < n; i++) {
                ecgData[i] = (short) ((value[i * 2] << 8) | (value[i * 2 + 1] & 0xFF));
            }
            Log.w("CCValue"," "+ecgData[0]);
            broadcastUpdate(ACTION_DATA_AVAILABLE, "ECG", ecgData,ecgUUID);
        }
    }

    private void decodeHRV(byte[] value) {
        if (value != null && value.length % 2 == 0){
            int n = value.length / 2;
            short[] hrvData = new short[n];
            for (int i = 1; i < n; i++) {
                hrvData[i] = (short) ((value[i * 2] << 8) | (value[i * 2 + 1] & 0xFF));
            }
           broadcastUpdate(ACTION_DATA_AVAILABLE, "HRV", hrvData,hrvUUID);
        }
    }


    private void decodeQRS(byte[] value) {
        if (value != null && value.length % 2 == 0) {
            int n = value.length / 2;
            short[] qrsData = new short[n];
            for (int i = 0; i < n; i++) {
                qrsData[i] = (short) ((value[i * 2] << 8) | (value[i * 2 + 1] & 0xFF));
            }
            broadcastUpdate(ACTION_DATA_AVAILABLE, "QRS", qrsData,qrsUUID);
        }
    }
    private void decodeTemperature(byte[] value) {
        if (value != null && value.length  == 4) {
            int temp = ((value[0] & 0xFF) << 24) |
                        ((value[1] & 0xFF) << 16) |
                        ((value[2] & 0xFF) << 8)  |
                        (value[3] & 0xFF);

            broadcastUpdate(ACTION_DATA_AVAILABLE, "TEMP", temp,temperatureUUID);
        }
    }

    private void runOnMainThread(Runnable task) {
        new Handler(getMainLooper()).post(task);
    }


    public boolean connect(String address) {
        if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {

            return false;
        }
        if (bluetoothAdapter == null || address == null) {
            Log.w("BLEConnect", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w("BLEConnect", "Device not found. Unable to connect.");
            return false;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d("BLEConnect", "Trying to create a new connection.");
        return true;
    }

    public void disconnect() {
        if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {

            return;
        }
        if (bluetoothGatt != null) bluetoothGatt.disconnect();
    }

    public void close() {
        if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {

            return;
        }
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {

            return null;
        }
        return bluetoothGatt.getServices();
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {

            return false;
        }
        if(characteristic.getValue() != null )Log.w("CCValue", "readCharacterisitcs"+characteristic.getValue().toString());
        else Log.w("CCValue", "readCharacterisitcs null");
        return (bluetoothGatt != null) ? bluetoothGatt.readCharacteristic(characteristic) :  false;
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {

            return;
        }
        characteristic.setValue(value);
        if (bluetoothGatt != null) bluetoothGatt.writeCharacteristic(characteristic);
    }

    public void enableNotifications(BluetoothGattCharacteristic characteristic, UUID uuid) {
        Log.d("CCValue", "Inside Notification enabled.");
        if (bluetoothGatt == null) {
            Log.e("CCValue", "BluetoothGatt is null, cannot enable notifications.");
            return;
        }
        if (!PermissionsUtility.checkPermission(BluetoothLeService.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {
            return;
        }
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0 ) {
            Log.d("CCValue", "Characteristic does not support notifications or indications."+characteristic.getUuid().toString() );
            return;
        }
        // Enable notifications locally
        bluetoothGatt.setCharacteristicNotification(characteristic, true);
        Log.d("CCValue", "passed checks Notification enabled.");
        // Write to the Client Characteristic Configuration Descriptor (CCCD)
//
//        for (BluetoothGattDescriptor desc : characteristic.getDescriptors()) {
//            Log.d("CCValue", "Available Descriptor UUID: " + desc.getUuid());
//        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

//            if (descriptor == null) {
//                Log.e("CCValue", "CCCD Descriptor not found for characteristic: " + characteristic.getUuid());
//                for (BluetoothGattDescriptor desc : characteristic.getDescriptors()) {
//                    Log.d("CCValue", "Available Descriptor UUID: " + desc.getUuid());
//                }
//                return;
//            }

        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d("CCValue", "Notification enabled.");
            bluetoothGatt.writeDescriptor(descriptor);
        } else {
            Log.e("CCValue", "CCCD Descriptor not found for this characteristic.");
        }
    }

    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
        Intent intent = new Intent(action);
        byte[] value = characteristic.getValue();
        //Log.d("CCValue", "Broadcasting data: " + characteristic.getUuid() + " -> " + new String(value));
        if (value != null) {
            //intent.putExtra(BluetoothLeService.EXTRA_DATA, decodeTemperature(value));
        }
        //Intent intent = new Intent(ACTION_DATA_AVAILABLE);
        intent.putExtra(BluetoothLeService.EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(BluetoothLeService.EXTRA_DATA, "12");
        sendBroadcast(intent);

    }

    private void broadcastUpdate(String action, String dataType, Object data,String uuid) {
        Intent intent = new Intent(action);
        intent.putExtra(BluetoothLeService.EXTRA_UUID, uuid);
        intent.putExtra("DATA_TYPE", dataType);

        if (data instanceof short[]) {
            // Convert short[] to Short[]
            short[] shortArray = (short[]) data;
//            Short[] boxedArray = new Short[shortArray.length];
//            for (int i = 0; i < shortArray.length; i++) {
//                boxedArray[i] = shortArray[i];
//            }
            Log.w("CCValue" ," "+shortArray[0]);
            intent.putExtra("DATA_ARRAY_BOXED", shortArray);
        } else if (data instanceof byte[]) {
            intent.putExtra("DATA_ARRAY_BYTE", (byte[]) data);
        } else if (data instanceof Integer) {
            intent.putExtra("DATA_SINGLE", (Integer) data);
        }

        sendBroadcast(intent);
    }


}
