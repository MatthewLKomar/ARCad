package com.example.signalgateway

import com.example.signalgateway.MainActivity
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.widget.Toast
import java.util.UUID

class BLEManager(private val context: Context) {

    val SERVICE_UUID = "your-service-uuid"
    val CHARACTERISTIC_UUID = "your-characteristic-uuid"
    val DESCRIPTOR_UUID = "your-descriptor-uuid"
    val TARGET_MAC = "AA:BB:CC:DD:EE:FF"  // Replace this with the MAC address of the target device


    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

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
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt?.discoverServices()
            }
        }

        // Services discovered: now you can interact with the device's services and characteristics
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
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
        bluetoothLeScanner?.startScan(scanCallback)
    }

    // Connect to the Bluetooth device
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // Close the GATT connection when done
    @SuppressLint("MissingPermission")
    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
