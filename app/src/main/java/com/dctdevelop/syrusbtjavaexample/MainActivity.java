package com.dctdevelop.syrusbtjavaexample;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    Button sendButton;
    EditText texter;
    TextView peripheralTextView;
    Context context;
    BluetoothGatt gattDevice;
    BluetoothGattCharacteristic toWriteCharacteristic;
    UUID uuid = UUID.fromString("00000000-dc70-0080-dc70-a07ba85ee4d6");
    UUID uuid2 = UUID.fromString("00000000-dc70-0180-dc70-a07ba85ee4d6");
    UUID uuid3 = UUID.fromString("00000000-dc70-0280-dc70-a07ba85ee4d6");
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        texter = (EditText) findViewById(R.id.editText2);

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());


        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendText();
            }
        });




        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice().getName() != null && result.getDevice().getName().contains("Syrus")){
                peripheralTextView.append("Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
                btScanner.stopScan(leScanCallback);
                result.getDevice().connectGatt(context,true,connectCallback);
                final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0) {
                    // auto scroll for text view
                    peripheralTextView.scrollTo(0, scrollAmount);
                }
            }

        }
    };

    private BluetoothGattCallback  connectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("ble", "status:" + status);
            if(newState == 2) {
                peripheralTextView.append("Connected to Device: " + gatt.getDevice().getName() + "\n");
                gattDevice = gatt;
                gatt.discoverServices();
            }
            if(newState == 0) {
                peripheralTextView.append("Disconnected from Device: " + gatt.getDevice().getName() + "\n");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> list = gatt.getServices();
            for (int i =0; i < list.size(); i++){
                Log.d("service  ", "service: "+ list.get(i).getUuid());
                peripheralTextView.append("Service " + i +": " + list.get(i).getUuid()  + "\n");
            }
            List<BluetoothGattCharacteristic> list2 = gatt.getService(uuid).getCharacteristics();
            for (int j =0; j < list2.size(); j++){
                Log.d("characteristic  ", "characteristic: "+ list2.get(j).getUuid());
                peripheralTextView.append("Characteristic " + j +": " + list2.get(j).getUuid()  + "\n");
            }

            BluetoothGattCharacteristic characteristic =  gatt.getService(uuid).getCharacteristic(uuid2);
            toWriteCharacteristic = gatt.getService(uuid).getCharacteristic(uuid2);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

            peripheralTextView.append("writing Characteristic "+ characteristic.getUuid() + "\n");
            characteristic.setValue(">SBIK09792<".getBytes());
            gatt.writeCharacteristic(characteristic);


            /**
            List<BluetoothGattDescriptor> list3 = characteristic.getDescriptors();
            for (int k =0; k < list3.size(); k++){
                Log.d("descriptor  ", "descriptor: "+ list3.get(k).getUuid());
                peripheralTextView.append("descriptor " + k +": " + list3.get(k).getUuid()  + "\n");
            }**/



        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            peripheralTextView.append("Characteristic "+ characteristic.getUuid() + " writed:"+ characteristic.getStringValue(0)+"\n");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d("changeChar", "onCharacteristicChanged: " + characteristic.getStringValue(0));
            peripheralTextView.append("DATA: " + characteristic.getStringValue(0)+"\n");

        }

        /**
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("DescriptorWrite", "onDescriptorWrite: " + descriptor.getValue());
            gatt.readCharacteristic(toWriteCharacteristic);
        }
        **/
    };




    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        peripheralTextView.setText("");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void sendText(){
        String text = texter.getText().toString();
        toWriteCharacteristic.setValue((">"+text.toUpperCase()+"<").getBytes());
        gattDevice.writeCharacteristic(toWriteCharacteristic);
        texter.setText("");
    }
}