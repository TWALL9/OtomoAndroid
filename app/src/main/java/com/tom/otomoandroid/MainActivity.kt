package com.tom.otomoandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private val TAG: String = javaClass.name
    private val CONNECTING_STATUS = 1
    private val MESSAGE_READ = 2
    private var selectedDevice: String? = null
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var handler: Handler
    private lateinit var connectThread: ConnectThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var connectButton = findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener {

            val intent = Intent(this, SelectDeviceActivity::class.java)
            startActivity(intent)
        }

        var toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.subtitle = "Connect to device"

        selectedDevice = intent.getStringExtra("deviceName")
        if (selectedDevice != null) {
            val deviceAddress = intent.getStringExtra("deviceAddress")
            toolbar.subtitle = "Connecting to ${selectedDevice}..."
            connectButton.isEnabled = false

            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val createConnectThread = CreateConnectThread(btAdapter, deviceAddress)
            createConnectThread.start()
        }

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                Log.i(TAG, "Received message: ${msg.what}")
                when (msg.what) {
                    CONNECTING_STATUS -> when (msg.arg1) {
                        1 -> {
                            toolbar.subtitle = "Connected to ${selectedDevice}"
                        }
                        -1 -> {
                            toolbar.subtitle = "Failed to connect"
                        }
                    }
                    MESSAGE_READ -> {
                        // This is a message from the receiver
                        val received = msg.obj.toString().lowercase()
                        Log.i(TAG, "Received message: $received")
                    }
                }
            }
        }
    }

    inner class CreateConnectThread(var bluetoothAdapter: BluetoothAdapter, val address: String?) : Thread() {
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
        var tempSocket: BluetoothSocket? = null
        init {
            val uuid = bluetoothDevice.uuids[0].uuid
            try {
                tempSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e("ConnectThread", "Could not create socket")
            }

            bluetoothSocket = tempSocket!!
        }

        override fun run() {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            try {
                bluetoothSocket.connect()
                Log.i("ConnectThread", "Device connected")
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget()
            } catch (connectException: IOException) {
                try {
                    bluetoothSocket.close()
                    Log.e(TAG,"Could not connect to target", connectException)
                } catch (closeException: IOException) {
                    Log.e("ConnectThread", "Could not connect to device", closeException)
                }
                return
            }
            connectThread = ConnectThread(bluetoothSocket)
            connectThread.run()
        }
    }

    inner class ConnectThread(private val socket: BluetoothSocket) : Thread() {
        private var inputStream: InputStream = socket.inputStream
        private var outputstream: OutputStream = socket.outputStream

        override fun run() {
            var buffer = CharArray(1024)
            var bytes = 0

            while (true) {
                try {
                    buffer[bytes] = inputStream.read().toChar()
                    if (buffer[bytes] == '\n') {
                        val message = String(buffer, 0, bytes)
                        Log.i("Connect thread", "new message $message")
                        handler.obtainMessage(MESSAGE_READ,message).sendToTarget()
                        bytes = 0
                    } else {
                        bytes++
                    }
                } catch (e: IOException) {
                    Log.e("ConnectThread", "IO Error!", e)
                    break
                }
            }
        }
    }
}