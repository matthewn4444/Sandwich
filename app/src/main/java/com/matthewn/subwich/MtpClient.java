package com.matthewn.subwich;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MtpClient {
    private static final String TAG = "MtpClient";
    private final Context mContext;
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    // mDevices contains all MtpDevices that have been seen by our client,
    // so we can inform when the device has been detached.
    // mDevices is also used for synchronization in this class.
    private final HashMap<String, UsbDevice> mDevices = new HashMap<>();
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            String deviceName = usbDevice.getDeviceName();
            if (isMassStorage(usbDevice)) {
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    synchronized (mDevices) {
                        mDevices.put(deviceName, usbDevice);
                    }
                    for (Listener listener : mListeners) {
                        listener.deviceAdded(usbDevice);
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    synchronized (mDevices) {
                        mDevices.remove(deviceName);
                    }
                    for (Listener listener : mListeners) {
                        listener.deviceRemoved(usbDevice);
                    }
                }
            }
        }
    };

    boolean isMassStorage(UsbDevice device) {
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_MASS_STORAGE &&
                    intf.getInterfaceSubclass() == 6 &&
                    intf.getInterfaceProtocol() == 80 &&
                    intf.getEndpointCount() == 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * An interface for being notified when MTP or PTP devices are attached
     * or removed.  In the current implementation, only PTP devices are supported.
     */
    public interface Listener {
        /**
         * Called when a new device has been added
         *
         * @param device the new device that was added
         */
        public void deviceAdded(UsbDevice device);

        /**
         * Called when a new device has been removed
         *
         * @param device the device that was removed
         */
        public void deviceRemoved(UsbDevice device);
    }

    /**
     * MtpClient2 constructor
     *
     * @param context the {@link android.content.Context} to use for the MtpClient2
     */
    public MtpClient(Context context) {
        mContext = context;
    }

    public void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mUsbReceiver, filter);

        // Check for any devices added
        synchronized (mDevices) {
            mDevices.clear();
            UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            for (UsbDevice device: deviceList.values()) {
                if (isMassStorage(device)) {
                    mDevices.put(device.getDeviceName(), device);
                }
            }
        }
    }

    public void unregister() {
        mContext.unregisterReceiver(mUsbReceiver);
    }

    /**
     * Registers a {@link android.mtp.MtpClient.Listener} interface to receive
     * notifications when MTP or PTP devices are added or removed.
     *
     * @param listener the listener to register
     */
    public void addListener(Listener listener) {
        synchronized (mDevices) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a {@link android.mtp.MtpClient.Listener} interface.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(Listener listener) {
        synchronized (mDevices) {
            mListeners.remove(listener);
        }
    }

    /**
     * Retrieves a list of all currently connected {@link android.mtp.MtpDevice}.
     *
     * @return the list of MtpDevices
     */
    public List<UsbDevice> getDeviceList() {
        synchronized (mDevices) {
            return new ArrayList<>(mDevices.values());
        }
    }
}
