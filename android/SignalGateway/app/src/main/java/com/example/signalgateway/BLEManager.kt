package com.example.signalgateway

import com.example.signalgateway.MainActivity
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.widget.Toast
import java.util.UUID

class BLEManager(private val context: Context) {

    val SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"   // Nordic UART Service
    val CHARACTERISTIC_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"    // TX Characteristic
    val DESCRIPTOR_UUID = "0x2902"
    val TARGET_MAC = "DF:AA:D6:83:21:4F"  // Replace this with the MAC address of the target device


    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    // Create ScanFilter for the target MAC address
    val scanFilter = ScanFilter.Builder()
        .setDeviceAddress(TARGET_MAC) // Set the target MAC address
        .build()

    // Create ScanSettings for BLE scan
    val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Set scan mode
        .build()

    private val scanCallback = object : android.bluetooth.le.ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (device.address == TARGET_MAC) {
                    bluetoothLeScanner?.stopScan(this) // Stop scanning after finding a device
                    connectToDevice(device)
                    Toast.makeText(context, "Found device $TARGET_MAC", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(context, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        // Connection state change: when the connection state changes, try to discover services
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Toast.makeText(context, "onConnectionStateChange", Toast.LENGTH_SHORT).show()
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Toast.makeText(context, "Connected to device", Toast.LENGTH_SHORT).show()
                gatt?.discoverServices()
            }
        }

        // Services discovered: now you can interact with the device's services and characteristics
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Toast.makeText(context, "Services discovered. Looking for characteristics.", Toast.LENGTH_SHORT).show()
                val serviceUUID = SERVICE_UUID
                val characteristicUUID = CHARACTERISTIC_UUID
                val service = gatt?.getService(UUID.fromString(serviceUUID))
                val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUUID))

                characteristic?.let {
                    // Enable notifications for the characteristic
                    gatt.setCharacteristicNotification(it, true)

                    // Write descriptor to enable notifications
                    val descriptor = it.getDescriptor(UUID.fromString(DESCRIPTOR_UUID))

                    // Write the descriptor asynchronously
                    val success = gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    if (success != BluetoothStatusCodes.SUCCESS) {
                        // Handle failure in writing descriptor
                        Toast.makeText(context, "Failed to write descriptor", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Descriptor write callback: handle the response after writing a descriptor
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Descriptor written successfully
                Toast.makeText(context, "Descriptor written successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Descriptor write failed
                Toast.makeText(context, "Failed to write descriptor", Toast.LENGTH_SHORT).show()
            }
        }

        // Characteristic notification callback: called when the characteristic's value changes
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
                val hexString = value.joinToString("") { byte -> "%02x".format(byte) }
                // Process the received data (for example, send it to the UI)
                (context as? MainActivity)?.onDataReceived(hexString)
        }
    }

    // Function to start scanning for Bluetooth devices
    @SuppressLint("MissingPermission")
    fun scanForDevices() {
        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }

    // Connect to the Bluetooth device
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        Toast.makeText(context, "Connecting to device...", Toast.LENGTH_SHORT).show()
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    // Close the GATT connection when done
    @SuppressLint("MissingPermission")
    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
