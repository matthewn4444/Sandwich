package com.matthewn.subwich;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public abstract class UsbDetectionActivity extends BaseActivity {
    private static final String TAG = "UsbDetectionActivity";
    protected static final int REQUEST_CODE_SETUP_USB = 1;

    private final List<Uri> mDevices = new ArrayList<>();
    private Spinner mExtDevicesSpinner;
    private ArrayAdapter<String> mAdapter;
    private Dialog mUsbDeviceAvailableDialog;
    private MtpClient mMtpClient;
    private int mConnectionWaitTries;
    private Handler mConnectionCheckHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExtDevicesSpinner = (Spinner) findViewById(R.id.externaldevicespinner);
        mAdapter = new ArrayAdapter<>(this, R.layout.devices_spinner_item_layout);
        mConnectionWaitTries = 0;

        mMtpClient = new MtpClient(this);
        mMtpClient.addListener(new MtpClient.Listener() {
            @Override
            public void deviceAdded(UsbDevice device) {
                waitForDeviceToConnect();
            }

            @Override
            public void deviceRemoved(UsbDevice device) {
                updateUsbDevicesListing();
            }
        });
    }

    @Override
    protected void onResume() {
        mMtpClient.register();
        if (!hasStartedForResult()) {
            waitForDeviceToConnect();
        } else {
            updateUsbDevicesListing();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMtpClient.unregister();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETUP_USB && resultCode == RESULT_OK) {
            Uri treeUri = data.getData();

            // Make sure user chose the root of the device and also not internal storage
            String[] path = treeUri.getLastPathSegment().split(":");
            if (path.length == 1) {
                if (!path[0].equalsIgnoreCase("primary")) {
                    // Check if we already have this permission
                    final ContentResolver resolver = getContentResolver();
                    for (UriPermission permission: resolver.getPersistedUriPermissions()) {
                        if (permission.getUri().getPath().equals(treeUri.getPath())) {
                            Toast.makeText(this,
                                    R.string.message_request_storage_permission_granted_already,
                                    Toast.LENGTH_LONG).show();
                            showUsbDeviceAvaliableDialog();
                            return;
                        }
                    }

                    // Save the uri permissions
                    resolver.takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    updateUsbDevicesListing();
                } else {
                    Toast.makeText(this,
                            R.string.message_request_storage_permission_primary_storage_warning,
                            Toast.LENGTH_LONG).show();
                    showUsbDeviceAvaliableDialog();
                }
            } else {
                Toast.makeText(this,
                        R.string.message_request_storage_permission_invalid_storage_warning,
                        Toast.LENGTH_LONG).show();
                showUsbDeviceAvaliableDialog();
            }
        }

    }

    private void waitForDeviceToConnect() {
        mConnectionCheckHandler.removeCallbacksAndMessages(null);
        mConnectionWaitTries++;
        updateUsbDevicesListing();

        // Leave if a device has connected or there are no outstanding devices to connect
        if (mDevices.size() >= mMtpClient.getDeviceList().size()) {
            mConnectionWaitTries = 0;
        } else {
            if (mConnectionWaitTries > 10
                    || getContentResolver().getPersistedUriPermissions().isEmpty()) {
                Log.v(TAG, "Timeout trying to detect if usb attached has permissions");
                mConnectionWaitTries = 0;
                updateUsbDevicesListing(true);
            } else {
                mConnectionCheckHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        waitForDeviceToConnect();
                    }
                }, 500);
            }
        }
    }

    protected Uri getSelectedDevice() {
        if (mDevices.isEmpty()) {
            return null;
        }
        return mDevices.get(mExtDevicesSpinner.getSelectedItemPosition());
    }

    protected void updateUsbDevicesListing() {
        updateUsbDevicesListing(false);
    }

    protected void updateUsbDevicesListing(boolean showDialog) {
        synchronized (mDevices) {
            mDevices.clear();

            // Check the permitted devices and get which are connected
            List<UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
            for (UriPermission permission : permissions) {
                DocumentFile docDir = DocumentFile.fromTreeUri(this, permission.getUri());
                if (docDir.exists()) {
                    mDevices.add(permission.getUri());
                }
            }
            Log.v(TAG, "There are " + mDevices.size() + " saved devices connected");
        }

        // Check if we have more devices connected than saved permissions, ask to get more
        List<UsbDevice> devices = mMtpClient.getDeviceList();
        if (mDevices.size() < devices.size() && showDialog) {
            showUsbDeviceAvaliableDialog();
        } else {
            spinnerUpdated();
        }
    }

    private void showUsbDeviceAvaliableDialog() {
        if (mUsbDeviceAvailableDialog == null) {
            mUsbDeviceAvailableDialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.message_usb_device_available)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectFolder(REQUEST_CODE_SETUP_USB);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            spinnerUpdated();
                        }
                    })
                    .create();
        }
        mUsbDeviceAvailableDialog.show();
    }

    private void spinnerUpdated() {
        mAdapter.clear();
        if (mDevices.isEmpty()) {
            mAdapter.add(getString(R.string.message_no_device_connected));
        } else {
            for (Uri uri: mDevices) {
                String[] parts = uri.getLastPathSegment().split(":");
                if (parts.length == 1) {
                    mAdapter.add(parts[0]);
                } else {
                    Log.w(TAG, "Cannot add uri permission to list, cannot find name: " + uri.toString());
                }
            }

            // TODO maybe add logic to select the last usbdevice from shared pref?
        }
        mExtDevicesSpinner.setEnabled(!mDevices.isEmpty());
        mExtDevicesSpinner.setAdapter(mAdapter);
    }
}
