package mws.com.bluetoothle;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import static mws.com.bluetoothle.Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID;
import static mws.com.bluetoothle.Constants.HEART_RATE_SERVICE_UUID;
import static mws.com.bluetoothle.Constants.SERVER_MSG_FIRST_STATE;
import static mws.com.bluetoothle.Constants.SERVER_MSG_SECOND_STATE;

public class DeviceConnectActivity extends BluetoothActivity implements View.OnClickListener {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private CentralService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mDeviceServices;
    private BluetoothGattCharacteristic mCharacteristic;

    private String mDeviceName;
    private String mDeviceAddress;

    private TextView mConnectionStatus;
    private TextView mConnectedDeviceName;
    private TextView mServerCharacteristic;
    private Button mRequestReadCharacteristic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeviceServices = new ArrayList<>();
        mCharacteristic = null;

        Intent intent = getIntent();
        if (intent != null) {
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        }

        mConnectionStatus = findViewById(R.id.connection_status);
        mConnectedDeviceName = findViewById(R.id.connected_device_name);
        mServerCharacteristic = findViewById(R.id.server_characteristic_value);
        mRequestReadCharacteristic = findViewById(R.id.request_read_characteristic);
        mRequestReadCharacteristic.setOnClickListener(this);


        if (TextUtils.isEmpty(mDeviceName)) {
            mConnectedDeviceName.setText("");
        } else {
            mConnectedDeviceName.setText(mDeviceName);
        }

        Intent gattServiceIntent = new Intent(this, CentralService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_device_connect;
    }

    @Override
    protected int getTitleString() {
        return R.string.central_connection_screen;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.request_read_characteristic:
                requestReadCharacteristic();
                break;
        }
    }

    private void requestReadCharacteristic() {
        if (mBluetoothLeService != null && mCharacteristic != null) {
            mBluetoothLeService.readCharacteristic(mCharacteristic);
        } else {
            showMsgText(R.string.error_unknown);
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((CentralService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(MainActivity.TAG, "Unable to initialize Bluetooth");
                finish();
            }

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /*
     Handles various events fired by the Service.
     ACTION_GATT_CONNECTED: connected to a GATT server.
     ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action == null) {
                return;
            }

            switch (intent.getAction()) {

                case CentralService.ACTION_GATT_CONNECTED:
                    updateConnectionState(R.string.connected);
                    mRequestReadCharacteristic.setEnabled(true);
                    break;

                case CentralService.ACTION_GATT_DISCONNECTED:
                    updateConnectionState(R.string.disconnected);
                    mRequestReadCharacteristic.setEnabled(false);
                    break;


                case CentralService.ACTION_GATT_SERVICES_DISCOVERED:
                    // set all the supported services and characteristics on the user interface.
                    setGattServices(mBluetoothLeService.getSupportedGattServices());
                    registerCharacteristic();
                    break;

                case CentralService.ACTION_DATA_AVAILABLE:
                    String msg = intent.getStringExtra(CentralService.EXTRA_DATA);
                    Log.v(MainActivity.TAG, "ACTION_DATA_AVAILABLE " + msg);
                    updateInputFromServer(msg);
                    break;
            }
        }
    };

    private void registerCharacteristic() {

        BluetoothGattCharacteristic characteristic = null;

        if (mDeviceServices != null) {

            /* iterate all the Services the connected device offer.
            a Service is a collection of Characteristic.
             */
            for (ArrayList<BluetoothGattCharacteristic> service : mDeviceServices) {

                // iterate all the Characteristic of the Service
                for (BluetoothGattCharacteristic serviceCharacteristic : service) {

                    /* check this characteristic belongs to the Service defined in
                    PeripheralAdvertiseService.buildAdvertiseData() method
                     */
                    if (serviceCharacteristic.getService().getUuid().equals(HEART_RATE_SERVICE_UUID)) {

                        if (serviceCharacteristic.getUuid().equals(BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID)) {
                            characteristic = serviceCharacteristic;
                            mCharacteristic = characteristic;
                        }
                    }
                }
            }

           /*
            int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            */

            if (characteristic != null) {
                mBluetoothLeService.readCharacteristic(characteristic);
                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
            }
        }
    }

    private void setGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null) {
            return;
        }

        mDeviceServices = new ArrayList<>();

        // Loops through available GATT Services from the connected device
        for (BluetoothGattService gattService : gattServices) {
            ArrayList<BluetoothGattCharacteristic> characteristic = new ArrayList<>();
            characteristic.addAll(gattService.getCharacteristics());
            mDeviceServices.add(characteristic);
        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionStatus.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CentralService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(CentralService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(CentralService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(CentralService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

//    NOTE: This is where the colour is being updated in client
    @SuppressLint("StringFormatMatches")
    private void updateInputFromServer(String msg) {
        mServerCharacteristic.setText(msg);
        showMsgText(String.format(getString(R.string.characteristic_value_received), msg));
    }
}
