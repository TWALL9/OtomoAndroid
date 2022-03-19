package com.tom.otomoandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.ArrayList

class SelectDeviceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_selector)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        var pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        var deviceList = ArrayList<DeviceInfo>();
        var deviceListAdapter = DeviceListAdapter(this, deviceList)

        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                val deviceName = device.name
                val hardwareAddress = device.address
                deviceList.add(DeviceInfo(deviceName, hardwareAddress))
            }

            var recyclerView = findViewById<RecyclerView>(R.id.deviceRecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = deviceListAdapter
            recyclerView.itemAnimator = DefaultItemAnimator()
        } else {
            val view = findViewById<RecyclerView>(R.id.deviceRecyclerView)
            var snackbar = Snackbar.make(view, "Cannot find Bluetooth devices", Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction("OK") { }
            snackbar.show()
        }
    }
}