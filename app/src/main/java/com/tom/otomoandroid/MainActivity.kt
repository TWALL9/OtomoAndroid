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
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import io.github.controlwear.virtual.joystick.android.JoystickView
import io.github.controlwear.virtual.joystick.android.JoystickView.OnMoveListener
import java.io.*


class MainActivity : AppCompatActivity() {
    private val TAG: String = javaClass.name
    private val CONNECTING_STATUS = 1
    private val MESSAGE_READ = 2
    private val CONNECT_SUCCESS = 1
    private val CONNECT_FAIL = -1
    private var selectedDevice: String? = null
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var handler: Handler
    private lateinit var connectThread: ConnectThread

    private var mAngle = 0
    private var mStrength = 0

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

        var sendButton = findViewById<Button>(R.id.sendButton)
        sendButton.isEnabled = false

        var sendTextBox = findViewById<EditText>(R.id.sendText)
        sendTextBox.isEnabled = false

        sendButton.setOnClickListener {
            sendButton.isEnabled = false
            connectThread.send(sendTextBox.text.toString() + "\n")
            sendButton.isEnabled = true
            sendTextBox.text.clear()
        }

        selectedDevice = intent.getStringExtra("deviceName")
        if (selectedDevice != null) {
            val deviceAddress = intent.getStringExtra("deviceAddress")
            toolbar.subtitle = "Connecting to ${selectedDevice}..."
            connectButton.isEnabled = false

            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val createConnectThread = CreateConnectThread(btAdapter, deviceAddress)
            createConnectThread.start()
        }

        val strengthView = findViewById<TextView>(R.id.strength_id)
        val angleView = findViewById<TextView>(R.id.angle_id)

        val joystickView = findViewById<JoystickView>(R.id.joystickView_left)
        joystickView.setOnMoveListener(OnMoveListener { angle, strength ->
            angleView.text = "$angle°"
            strengthView.text = "$strength%"
            if (sendButton.isEnabled && (mAngle != angle || mStrength != strength)) {
                Log.d(TAG, "$angle°, $strength%")
                mAngle = angle
                mStrength = strength
                connectThread.send("$angle\n")
            }
        })

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                when (msg.what) {
                    CONNECTING_STATUS -> {
                        Log.i(TAG, "Received message type: ${msg.what}, ${msg.arg1}")
                        when (msg.arg1) {
                            CONNECT_SUCCESS -> {
                                toolbar.subtitle = "Connected to $selectedDevice"
                                sendTextBox.isEnabled = true
                                sendButton.isEnabled = true
                            }
                            CONNECT_FAIL -> {
                                toolbar.subtitle = "Failed to connect"
                            }
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
//            val adapter = BluetoothAdapter.getDefaultAdapter()
            try {
                bluetoothSocket.connect()
                Log.i("ConnectThread", "Device connected")
                handler.obtainMessage(CONNECTING_STATUS, CONNECT_SUCCESS, CONNECT_FAIL).sendToTarget()
            } catch (connectException: IOException) {
                try {
                    bluetoothSocket.close()
                    Log.e(TAG,"Could not connect to target", connectException)
                } catch (closeException: IOException) {
                    Log.e("ConnectThread", "Could not close socket", closeException)
                } finally {
                    handler.obtainMessage(CONNECTING_STATUS, CONNECT_FAIL).sendToTarget()
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
            var bufferedReader = BufferedReader(InputStreamReader(inputStream))
            while (true) {
                try {
                    val message = bufferedReader.readLine()
//                    var buffer = ByteArray(1024)
//                    val message = String(buffer, Charset.defaultCharset())
                    Log.i("Connect thread", "new message $message")
                    handler.obtainMessage(MESSAGE_READ, message).sendToTarget()
                } catch (e: IOException) {
                    Log.e("ConnectThread", "IO Error!", e)
                    break
                }
            }
        }

        fun send(input: String) {
            val bytesToSend = input.toByteArray()
            try {
                outputstream.write(bytesToSend)
            } catch (e: IOException) {
                Log.e("ConnectThread", "unable to send", e)
            }
        }

        fun close() {
            try {
                socket.close()
            } catch (e: IOException) {
                // explode
            }
        }
    }
}