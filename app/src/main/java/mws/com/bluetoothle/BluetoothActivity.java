package mws.com.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public abstract class BluetoothActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        mToolbar = findViewById(R.id.toolbar);
        setToolbar();
    }

    protected abstract int getLayoutId();

    protected abstract int getTitleString();

    protected void onBackButtonClicked() {
        onBackPressed();
    }

    protected void showMsgText(int stringId) {
        showMsgText(getString(stringId));
    }

    protected void showMsgText(String string) {
        if (mToolbar != null) {
            Snackbar snackbar = Snackbar.make(mToolbar, string, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    protected BluetoothAdapter getBluetoothAdapter() {
        BluetoothAdapter bluetoothAdapter;
        BluetoothManager bluetoothService = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));

        if (bluetoothService != null) {
            bluetoothAdapter = bluetoothService.getAdapter();
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled()) {
                    /*
                    all the other Bluetooth initial checks already verified in MainActivity
                     */
                    return bluetoothAdapter;
                }
            }
        }
        return null;
    }

    private void setToolbar() {
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackButtonClicked();
            }
        });
        mToolbar.setTitle(getTitleString());
        mToolbar.setTitleTextColor(Color.WHITE);
    }
}
