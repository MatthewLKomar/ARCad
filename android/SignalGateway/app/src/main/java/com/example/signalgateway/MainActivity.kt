package com.example.signalgateway

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.pm.PackageManager

// toast/ble imports
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// ktor imports
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty


import com.example.signalgateway.BLEManager

class MainActivity : AppCompatActivity() {

    private lateinit var bleManager: BLEManager

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                bleManager.scanForDevices()
            } else {
                Toast.makeText(this, "Permissions are required to use Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Toast.makeText(this, "Hello World!", Toast.LENGTH_LONG).show()

//        // Start web server
//        embeddedServer(Netty, 5000) {
//            install(ContentNegotiation) {
//                gson {}
//            }
//            routing {
//                get("/") {
//                    call.respond(mapOf("message" to "Hello world"))
//                }
//            }
//        }.start(wait = true)
//
        // Initialize the BLEManager
        bleManager = BLEManager(this)

        // Check and request Bluetooth permissions
        requestBluetoothPermissions()
    }

    // Request Bluetooth permissions at runtime
    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        } else {
            Toast.makeText(this, "Scanning for devices", Toast.LENGTH_SHORT).show()
            bleManager.scanForDevices()
        }
    }

    // Callback to handle data received from the BLE device
    fun onDataReceived(data: String) {
        // Handle the received raw data (e.g., display it in a Toast)
        Toast.makeText(this, "Received data: $data", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the BLE connection when the activity is destroyed
        bleManager.close()
    }
}
